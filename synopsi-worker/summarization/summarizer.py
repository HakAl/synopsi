import logging
from typing import Optional, Tuple
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM

logger = logging.getLogger(__name__)

# Default model - lightweight and good for summarization
DEFAULT_MODEL = "sshleifer/distilbart-cnn-12-6"

# Length configurations based on SummaryLength enum
LENGTH_CONFIG = {
    'SHORT': {
        'min_length': 30,
        'max_length': 80,
        'target_words': '50-100'
    },
    'MEDIUM': {
        'min_length': 80,
        'max_length': 150,
        'target_words': '100-200'
    },
    'LONG': {
        'min_length': 150,
        'max_length': 300,
        'target_words': '200-400'
    }
}

# Type-specific prompts (for future use with instruction-tuned models)
TYPE_CONFIG = {
    'BRIEF': {
        'description': 'concise overview',
    },
    'DETAILED': {
        'description': 'comprehensive summary',
    },
    'ELI5': {
        'description': 'simple explanation',
    },
    'LIST': {
        'description': 'key points list',
    },
    'CUSTOM': {
        'description': 'custom summary',
    }
}


class Summarizer:
    """
    NLP model wrapper for text summarization using HuggingFace transformers.
    Uses DistilBART by default for efficient summarization.
    """

    def __init__(self, model_name: str = None):
        """
        Initialize the summarizer with a HuggingFace model.

        Args:
            model_name: HuggingFace model identifier (default: distilbart-cnn-12-6)
        """
        self.model_name = model_name or DEFAULT_MODEL
        self.device = self._get_device()
        self.tokenizer = None
        self.model = None

        logger.info(f"Initializing summarizer with model: {self.model_name}")
        logger.info(f"Using device: {self.device}")

        self._load_model()

    def _get_device(self) -> str:
        """Determine the best available device."""
        if torch.cuda.is_available():
            return "cuda"
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            return "mps"  # Apple Silicon
        return "cpu"

    def _load_model(self):
        """Load the tokenizer and model from HuggingFace."""
        try:
            logger.info(f"Loading tokenizer: {self.model_name}")
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)

            logger.info(f"Loading model: {self.model_name}")
            self.model = AutoModelForSeq2SeqLM.from_pretrained(self.model_name)
            self.model.to(self.device)
            self.model.eval()  # Set to evaluation mode

            logger.info(f"Model loaded successfully on {self.device}")

        except Exception as e:
            logger.error(f"Failed to load model {self.model_name}: {e}", exc_info=True)
            raise RuntimeError(f"Could not load summarization model: {e}")

    def summarize(
        self,
        text: str,
        summary_type: str = 'BRIEF',
        summary_length: str = 'MEDIUM'
    ) -> Tuple[str, int]:
        """
        Generate a summary for the given text.

        Args:
            text: Input text to summarize
            summary_type: Type of summary (BRIEF, DETAILED, ELI5, LIST, CUSTOM)
            summary_length: Length of summary (SHORT, MEDIUM, LONG)

        Returns:
            Tuple of (summary_text, token_count)

        Raises:
            ValueError: If text is empty
            RuntimeError: If summarization fails
        """
        if not text or not text.strip():
            raise ValueError("Cannot summarize empty text")

        # Get length configuration
        length_config = LENGTH_CONFIG.get(summary_length.upper(), LENGTH_CONFIG['MEDIUM'])
        min_length = length_config['min_length']
        max_length = length_config['max_length']

        logger.debug(f"Summarizing with type={summary_type}, length={summary_length}")
        logger.debug(f"Min tokens: {min_length}, Max tokens: {max_length}")

        try:
            # Tokenize input
            inputs = self.tokenizer(
                text,
                max_length=1024,
                truncation=True,
                return_tensors="pt"
            ).to(self.device)

            input_token_count = inputs['input_ids'].shape[1]
            logger.debug(f"Input token count: {input_token_count}")

            # Generate summary
            with torch.no_grad():
                summary_ids = self.model.generate(
                    inputs['input_ids'],
                    attention_mask=inputs['attention_mask'],
                    min_length=min_length,
                    max_length=max_length,
                    num_beams=4,
                    length_penalty=2.0,
                    early_stopping=True,
                    no_repeat_ngram_size=3
                )

            # Decode summary
            summary = self.tokenizer.decode(
                summary_ids[0],
                skip_special_tokens=True,
                clean_up_tokenization_spaces=True
            )

            output_token_count = summary_ids.shape[1]
            logger.info(f"Generated summary: {len(summary)} chars, {output_token_count} tokens")

            return summary, output_token_count

        except Exception as e:
            logger.error(f"Summarization failed: {e}", exc_info=True)
            raise RuntimeError(f"Failed to generate summary: {e}")

    def get_model_version(self) -> str:
        """
        Get the model identifier string.

        Returns:
            Model name/version string
        """
        return self.model_name

    def is_ready(self) -> bool:
        """
        Check if the model is loaded and ready.

        Returns:
            True if model is ready, False otherwise
        """
        return self.model is not None and self.tokenizer is not None

    def get_device_info(self) -> dict:
        """
        Get information about the device being used.

        Returns:
            Dictionary with device information
        """
        info = {
            'device': self.device,
            'model': self.model_name,
        }

        if self.device == 'cuda':
            info['cuda_device'] = torch.cuda.get_device_name(0)
            info['cuda_memory_allocated'] = f"{torch.cuda.memory_allocated() / 1024**2:.1f} MB"

        return info
