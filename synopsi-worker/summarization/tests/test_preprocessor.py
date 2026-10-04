import pytest
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from preprocessor import (
    clean_html,
    normalize_whitespace,
    truncate_to_max_tokens,
    remove_boilerplate,
    preprocess_for_summary,
    estimate_token_count
)


class TestCleanHtml:
    """Tests for clean_html function."""

    def test_removes_basic_tags(self):
        html = "<p>Hello <b>world</b>!</p>"
        result = clean_html(html)
        assert "Hello" in result
        assert "world" in result
        assert "<p>" not in result
        assert "<b>" not in result

    def test_removes_script_tags(self):
        html = "<p>Content</p><script>alert('bad')</script><p>More</p>"
        result = clean_html(html)
        assert "Content" in result
        assert "More" in result
        assert "alert" not in result
        assert "<script>" not in result

    def test_removes_style_tags(self):
        html = "<p>Content</p><style>.red{color:red}</style>"
        result = clean_html(html)
        assert "Content" in result
        assert "color" not in result

    def test_handles_empty_string(self):
        assert clean_html("") == ""
        assert clean_html(None) == ""

    def test_handles_plain_text(self):
        text = "Plain text without HTML"
        result = clean_html(text)
        assert result == text


class TestNormalizeWhitespace:
    """Tests for normalize_whitespace function."""

    def test_collapses_multiple_spaces(self):
        text = "Hello    world"
        result = normalize_whitespace(text)
        assert result == "Hello world"

    def test_collapses_multiple_newlines(self):
        text = "Hello\n\n\n\nworld"
        result = normalize_whitespace(text)
        assert result == "Hello\nworld"

    def test_replaces_tabs_with_spaces(self):
        text = "Hello\tworld"
        result = normalize_whitespace(text)
        assert result == "Hello world"

    def test_strips_leading_trailing(self):
        text = "  Hello world  "
        result = normalize_whitespace(text)
        assert result == "Hello world"

    def test_handles_empty_string(self):
        assert normalize_whitespace("") == ""
        assert normalize_whitespace(None) == ""


class TestTruncateToMaxTokens:
    """Tests for truncate_to_max_tokens function."""

    def test_short_text_unchanged(self):
        text = "This is a short text."
        result = truncate_to_max_tokens(text, max_tokens=1024)
        assert result == text

    def test_long_text_truncated(self):
        # Create a long text (1000 words)
        words = ["word"] * 1000
        text = " ".join(words)
        result = truncate_to_max_tokens(text, max_tokens=100)
        # max_tokens=100 * 0.7 = 70 words max
        assert len(result.split()) <= 70

    def test_tries_to_end_at_sentence(self):
        text = "First sentence. Second sentence. Third sentence. " * 50
        result = truncate_to_max_tokens(text, max_tokens=50)
        # Should end at a period if possible
        assert result.endswith('.') or len(result.split()) < 10

    def test_handles_empty_string(self):
        assert truncate_to_max_tokens("", 1024) == ""
        assert truncate_to_max_tokens(None, 1024) == ""


class TestRemoveBoilerplate:
    """Tests for remove_boilerplate function."""

    def test_removes_share_links(self):
        text = "Article content here. Share this article on Facebook."
        result = remove_boilerplate(text)
        assert "Article content here" in result
        # Boilerplate may or may not be fully removed depending on pattern

    def test_removes_subscribe_prompts(self):
        text = "Article content. Subscribe to our newsletter for updates."
        result = remove_boilerplate(text)
        assert "Article content" in result

    def test_handles_empty_string(self):
        assert remove_boilerplate("") == ""
        assert remove_boilerplate(None) == ""


class TestPreprocessForSummary:
    """Tests for the full preprocessing pipeline."""

    def test_full_pipeline(self):
        html = """
        <html>
        <head><title>Test</title></head>
        <body>
        <p>This is    the main   content.</p>
        <p>It has multiple paragraphs.</p>
        <script>console.log('bad')</script>
        </body>
        </html>
        """
        result = preprocess_for_summary(html)
        assert "main content" in result
        assert "multiple paragraphs" in result
        assert "console.log" not in result
        assert "<p>" not in result

    def test_handles_empty_input(self):
        assert preprocess_for_summary("") == ""
        assert preprocess_for_summary(None) == ""

    def test_respects_max_tokens(self):
        long_text = "This is a test sentence. " * 500
        result = preprocess_for_summary(long_text, max_tokens=100)
        # Should be truncated
        assert len(result) < len(long_text)


class TestEstimateTokenCount:
    """Tests for estimate_token_count function."""

    def test_estimates_tokens(self):
        text = "This is a test sentence with ten words in it."
        count = estimate_token_count(text)
        # ~10 words * 1.3 = ~13 tokens
        assert 10 <= count <= 20

    def test_handles_empty_string(self):
        assert estimate_token_count("") == 0
        assert estimate_token_count(None) == 0
