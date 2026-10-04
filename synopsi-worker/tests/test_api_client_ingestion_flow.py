"""
Regression tests for the ingestion worker API client defects:
- create_feed sent no title, which the API requires, so feed creation returned 400
- sources and feeds were never looked up before creating, so re-runs failed
- duplicate articles (409 from the API) were counted as failures
"""
from unittest.mock import Mock, patch

import logging

import pytest

from ingestion.api_client import SynopsiAPIClient


def _client():
    with patch('ingestion.api_client.SynopsiAPIClient._create_session') as mock_session:
        sess = Mock()
        sess.headers = {}
        mock_session.return_value = sess
        client = SynopsiAPIClient(base_url="http://localhost:8080", access_token="token")
    return client, sess


def _response(status, payload=None, text=""):
    resp = Mock()
    resp.status_code = status
    resp.json.return_value = payload
    resp.text = text
    return resp


class TestCreateFeedPayload:

    def test_create_feed_sends_a_non_blank_title(self):
        client, sess = _client()
        sess.request.return_value = _response(201, {'id': 7})

        client.create_feed("https://example.com/rss", source_id=3)

        _, kwargs = sess.request.call_args
        payload = kwargs['json']
        assert payload.get('title'), "FeedRequestDto requires a non-blank title"
        assert payload['sourceId'] == 3
        assert payload['feedUrl'] == "https://example.com/rss"


class TestEnsureSource:

    def test_reuses_existing_source_without_posting(self):
        client, sess = _client()
        sess.request.return_value = _response(200, {'id': 3, 'name': 'example.com'})

        source = client.ensure_source("https://example.com/rss")

        assert source['id'] == 3
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET']

    def test_creates_source_when_lookup_returns_404(self):
        client, sess = _client()
        sess.request.side_effect = [
            _response(404, text="not found"),
            _response(201, {'id': 4, 'name': 'example.com'}),
        ]

        source = client.ensure_source("https://example.com/rss")

        assert source['id'] == 4
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET', 'POST']


    @pytest.mark.parametrize("create_status, create_text", [
        (400, "Source with name 'example.com' already exists"),
        (409, "Request conflicts with an existing record"),
    ])
    def test_recovers_when_create_loses_a_race(self, create_status, create_text):
        client, sess = _client()
        sess.request.side_effect = [
            _response(404, text="not found"),
            _response(create_status, text=create_text),
            _response(200, {'id': 4, 'name': 'example.com'}),
        ]

        source = client.ensure_source("https://example.com/rss")

        assert source['id'] == 4
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET', 'POST', 'GET']

    def test_reraises_when_create_fails_and_source_still_missing(self):
        client, sess = _client()
        sess.request.side_effect = [
            _response(404, text="not found"),
            _response(400, text="Base URL must start with http:// or https://"),
            _response(404, text="not found"),
        ]

        with pytest.raises(ValueError):
            client.ensure_source("https://example.com/rss")


    def test_lost_race_does_not_log_an_error_traceback(self, caplog):
        client, sess = _client()
        sess.request.side_effect = [
            _response(404, text="not found"),
            _response(409, text="Request conflicts with an existing record"),
            _response(200, {'id': 4, 'name': 'example.com'}),
        ]

        with caplog.at_level(logging.INFO, logger='ingestion.api_client'):
            source = client.ensure_source("https://example.com/rss")

        assert source['id'] == 4
        errors = [r for r in caplog.records if r.levelno >= logging.ERROR]
        assert errors == [], f"a recovered race is not an error: {[r.message for r in errors]}"


class TestEnsureFeed:

    def test_reuses_existing_feed_for_source_without_posting(self):
        client, sess = _client()
        sess.request.return_value = _response(200, [
            {'id': 8, 'feedUrl': 'https://example.com/other'},
            {'id': 9, 'feedUrl': 'https://example.com/rss'},
        ])

        feed = client.ensure_feed("https://example.com/rss", source_id=3)

        assert feed['id'] == 9
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET']

    def test_creates_feed_when_source_has_no_matching_feed(self):
        client, sess = _client()
        sess.request.side_effect = [
            _response(200, []),
            _response(201, {'id': 10, 'feedUrl': 'https://example.com/rss'}),
        ]

        feed = client.ensure_feed("https://example.com/rss", source_id=3)

        assert feed['id'] == 10
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET', 'POST']


    @pytest.mark.parametrize("create_status, create_text", [
        (400, "Feed with URL already exists: https://example.com/rss"),
        (409, "Request conflicts with an existing record"),
    ])
    def test_recovers_when_feed_url_already_exists_under_another_source(self, create_status, create_text):
        client, sess = _client()
        sess.request.side_effect = [
            _response(200, []),
            _response(create_status, text=create_text),
            _response(200, [{'id': 12, 'sourceId': 1, 'feedUrl': 'https://example.com/rss'}]),
        ]

        feed = client.ensure_feed("https://example.com/rss", source_id=3)

        assert feed['id'] == 12
        methods = [call.args[0] for call in sess.request.call_args_list]
        assert methods == ['GET', 'POST', 'GET']

    def test_reraises_when_create_fails_and_feed_still_missing(self):
        client, sess = _client()
        sess.request.side_effect = [
            _response(200, []),
            _response(400, text="Crawl frequency is required"),
            _response(200, []),
        ]

        with pytest.raises(ValueError):
            client.ensure_feed("https://example.com/rss", source_id=3)

    def test_feed_title_is_capped_at_column_length(self):
        client, sess = _client()
        sess.request.return_value = _response(201, {'id': 7})
        with patch.object(client, '_extract_domain_from_url', return_value=''):
            client.create_feed("https://example.com/" + "x" * 500, source_id=3)

        _, kwargs = sess.request.call_args
        assert 0 < len(kwargs['json']['title']) <= 200


    def test_lost_race_does_not_log_an_error_traceback(self, caplog):
        client, sess = _client()
        sess.request.side_effect = [
            _response(200, []),
            _response(409, text="Request conflicts with an existing record"),
            _response(200, [{'id': 12, 'sourceId': 3, 'feedUrl': 'https://example.com/rss'}]),
        ]

        with caplog.at_level(logging.INFO, logger='ingestion.api_client'):
            feed = client.ensure_feed("https://example.com/rss", source_id=3)

        assert feed['id'] == 12
        errors = [r for r in caplog.records if r.levelno >= logging.ERROR]
        assert errors == []


class TestDuplicateArticles:

    def test_409_is_reported_as_duplicate_not_failure(self):
        client, sess = _client()
        sess.request.side_effect = [
            _response(201, {'id': 1}),
            _response(409, text='Article already exists with originalUrl: http://example.com/2'),
        ]
        articles = [
            {'title': 'A', 'originalUrl': 'http://example.com/1', 'feedId': 1},
            {'title': 'B', 'originalUrl': 'http://example.com/2', 'feedId': 1},
        ]

        result = client.create_articles_batch(articles)

        assert len(result['successful']) == 1
        assert len(result['failed']) == 0
        assert len(result['duplicates']) == 1
        assert result['duplicates'][0]['originalUrl'] == 'http://example.com/2'

    def test_duplicate_does_not_log_an_error_traceback(self, caplog):
        client, sess = _client()
        sess.request.return_value = _response(409, text='Article already exists')
        article = {'title': 'B', 'originalUrl': 'http://example.com/2', 'feedId': 1}

        with caplog.at_level(logging.INFO, logger='ingestion.api_client'):
            client.create_articles_batch([article])

        errors = [r for r in caplog.records if r.levelno >= logging.ERROR]
        assert errors == [], f"duplicates are expected on re-runs, got error logs: {[r.message for r in errors]}"

    def test_client_error_does_not_log_an_unexpected_error_traceback(self, caplog):
        client, sess = _client()
        sess.request.return_value = _response(400, text='Validation failed')
        article = {'title': 'B', 'originalUrl': 'http://example.com/2', 'feedId': 1}

        with caplog.at_level(logging.INFO, logger='ingestion.api_client'):
            result = client.create_articles_batch([article])

        assert len(result['failed']) == 1
        assert result['failed'][0]['error_type'] == 'validation'
        unexpected = [r for r in caplog.records if 'Unexpected error' in r.message]
        assert unexpected == []
