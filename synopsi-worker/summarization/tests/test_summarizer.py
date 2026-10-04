import pytest
import sys
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from summarizer import Summarizer, LENGTH_CONFIG, DEFAULT_MODEL


class TestSummarizerInit:
    """Tests for Summarizer initialization."""

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_init_loads_model(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False

        summarizer = Summarizer()

        mock_tokenizer.from_pretrained.assert_called_once_with(DEFAULT_MODEL)
        mock_model.from_pretrained.assert_called_once_with(DEFAULT_MODEL)

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_init_with_custom_model(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False
        custom_model = "facebook/bart-large-cnn"

        summarizer = Summarizer(model_name=custom_model)

        mock_tokenizer.from_pretrained.assert_called_once_with(custom_model)
        mock_model.from_pretrained.assert_called_once_with(custom_model)

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_uses_cuda_when_available(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = True

        summarizer = Summarizer()

        assert summarizer.device == "cuda"

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_uses_cpu_when_no_gpu(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False
        # Mock MPS check - set it up so hasattr returns False
        del mock_torch.backends.mps

        summarizer = Summarizer()

        assert summarizer.device == "cpu"


class TestSummarizerSummarize:
    """Tests for summarize method."""

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_summarize_returns_text_and_count(self, mock_torch, mock_tokenizer_cls, mock_model_cls):
        mock_torch.cuda.is_available.return_value = False
        mock_torch.no_grad.return_value.__enter__ = Mock()
        mock_torch.no_grad.return_value.__exit__ = Mock()

        # Mock tokenizer output - needs to be an object with .to() method
        mock_input_ids = Mock()
        mock_input_ids.shape = [1, 100]
        mock_attention_mask = Mock()

        mock_tokenizer_output = Mock()
        mock_tokenizer_output.__getitem__ = lambda self, key: mock_input_ids if key == 'input_ids' else mock_attention_mask
        mock_tokenizer_output.to = Mock(return_value=mock_tokenizer_output)

        # Mock tokenizer
        mock_tokenizer = Mock()
        mock_tokenizer.return_value = mock_tokenizer_output
        mock_tokenizer.decode.return_value = "This is a test summary."
        mock_tokenizer_cls.from_pretrained.return_value = mock_tokenizer

        # Mock model generate output
        mock_summary_ids = Mock()
        mock_summary_ids.shape = [1, 20]
        mock_summary_ids.__getitem__ = Mock(return_value=Mock())

        # Mock model
        mock_model = Mock()
        mock_model.generate.return_value = mock_summary_ids
        mock_model_cls.from_pretrained.return_value = mock_model

        summarizer = Summarizer()
        summary, token_count = summarizer.summarize("Test input text")

        assert summary == "This is a test summary."
        assert token_count == 20

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_summarize_empty_text_raises(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False

        summarizer = Summarizer()

        with pytest.raises(ValueError, match="Cannot summarize empty text"):
            summarizer.summarize("")

        with pytest.raises(ValueError, match="Cannot summarize empty text"):
            summarizer.summarize("   ")


class TestSummarizerGetModelVersion:
    """Tests for get_model_version method."""

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_returns_model_name(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False

        summarizer = Summarizer()
        version = summarizer.get_model_version()

        assert version == DEFAULT_MODEL

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_returns_custom_model_name(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False
        custom_model = "facebook/bart-large-cnn"

        summarizer = Summarizer(model_name=custom_model)
        version = summarizer.get_model_version()

        assert version == custom_model


class TestSummarizerIsReady:
    """Tests for is_ready method."""

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_is_ready_when_loaded(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False

        summarizer = Summarizer()
        assert summarizer.is_ready() is True

    @patch('summarizer.AutoModelForSeq2SeqLM')
    @patch('summarizer.AutoTokenizer')
    @patch('summarizer.torch')
    def test_not_ready_when_model_none(self, mock_torch, mock_tokenizer, mock_model):
        mock_torch.cuda.is_available.return_value = False

        summarizer = Summarizer()
        summarizer.model = None
        assert summarizer.is_ready() is False


class TestLengthConfig:
    """Tests for length configuration constants."""

    def test_short_config(self):
        config = LENGTH_CONFIG['SHORT']
        assert config['min_length'] < config['max_length']
        assert config['min_length'] >= 30
        assert config['max_length'] <= 100

    def test_medium_config(self):
        config = LENGTH_CONFIG['MEDIUM']
        assert config['min_length'] < config['max_length']
        assert config['min_length'] >= 80
        assert config['max_length'] <= 200

    def test_long_config(self):
        config = LENGTH_CONFIG['LONG']
        assert config['min_length'] < config['max_length']
        assert config['min_length'] >= 150
        assert config['max_length'] <= 400

    def test_all_lengths_defined(self):
        assert 'SHORT' in LENGTH_CONFIG
        assert 'MEDIUM' in LENGTH_CONFIG
        assert 'LONG' in LENGTH_CONFIG
