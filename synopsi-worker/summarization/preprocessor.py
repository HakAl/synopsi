import re
import logging
from typing import Optional
from bs4 import BeautifulSoup

logger = logging.getLogger(__name__)


def clean_html(text: str) -> str:
    """
    Remove HTML tags from text.

    Args:
        text: Text potentially containing HTML

    Returns:
        Clean text with HTML tags removed
    """
    if not text:
        return ""

    try:
        soup = BeautifulSoup(text, 'html.parser')

        # Remove script and style elements
        for element in soup(['script', 'style', 'head', 'meta', 'link']):
            element.decompose()

        # Get text content
        clean_text = soup.get_text(separator=' ')
        return clean_text

    except Exception as e:
        logger.warning(f"Error cleaning HTML: {e}")
        # Fallback: simple regex-based tag removal
        return re.sub(r'<[^>]+>', ' ', text)


def normalize_whitespace(text: str) -> str:
    """
    Normalize whitespace in text.
    - Replace multiple spaces/tabs with single space
    - Replace multiple newlines with single newline
    - Strip leading/trailing whitespace

    Args:
        text: Text to normalize

    Returns:
        Text with normalized whitespace
    """
    if not text:
        return ""

    # Replace tabs with spaces
    text = text.replace('\t', ' ')

    # Replace multiple spaces with single space
    text = re.sub(r' +', ' ', text)

    # Replace multiple newlines with single newline
    text = re.sub(r'\n+', '\n', text)

    # Replace newline followed/preceded by spaces
    text = re.sub(r' *\n *', '\n', text)

    # Strip leading/trailing whitespace
    return text.strip()


def truncate_to_max_tokens(text: str, max_tokens: int = 1024) -> str:
    """
    Truncate text to approximately max_tokens.
    Uses simple word-based estimation (1 token ~ 0.75 words).

    Args:
        text: Text to truncate
        max_tokens: Maximum number of tokens (default 1024 for BART models)

    Returns:
        Truncated text
    """
    if not text:
        return ""

    # Estimate: ~0.75 words per token, so max_tokens * 0.75 words
    # Being conservative, use max_tokens * 0.7 words
    max_words = int(max_tokens * 0.7)

    words = text.split()
    if len(words) <= max_words:
        return text

    logger.debug(f"Truncating text from {len(words)} to {max_words} words")
    truncated = ' '.join(words[:max_words])

    # Try to end at sentence boundary
    last_period = truncated.rfind('.')
    last_question = truncated.rfind('?')
    last_exclaim = truncated.rfind('!')

    last_sentence = max(last_period, last_question, last_exclaim)

    # Only truncate at sentence if we keep at least 70% of allowed content
    if last_sentence > len(truncated) * 0.7:
        truncated = truncated[:last_sentence + 1]

    return truncated


def remove_boilerplate(text: str) -> str:
    """
    Remove common boilerplate text from articles.

    Args:
        text: Article text

    Returns:
        Text with boilerplate removed
    """
    if not text:
        return ""

    # Common boilerplate patterns to remove
    patterns = [
        r'Share this article.*?(?=\n|$)',
        r'Follow us on.*?(?=\n|$)',
        r'Subscribe to.*?(?=\n|$)',
        r'Sign up for.*?(?=\n|$)',
        r'Advertisement\s*',
        r'Sponsored content\s*',
        r'Read more:.*?(?=\n|$)',
        r'Related articles?:.*?(?=\n|$)',
        r'Tags?:.*?(?=\n|$)',
        r'Comments?\s*\(\d+\)',
        r'©.*?(?=\n|$)',
        r'All rights reserved.*?(?=\n|$)',
    ]

    for pattern in patterns:
        text = re.sub(pattern, '', text, flags=re.IGNORECASE)

    return text


def preprocess_for_summary(
    text: str,
    max_tokens: int = 1024,
    remove_boilerplate_text: bool = True
) -> str:
    """
    Full preprocessing pipeline for summarization.

    Args:
        text: Raw article text (may contain HTML)
        max_tokens: Maximum tokens for model input
        remove_boilerplate_text: Whether to remove boilerplate

    Returns:
        Clean, normalized, truncated text ready for summarization
    """
    if not text:
        logger.warning("Empty text provided for preprocessing")
        return ""

    # Step 1: Clean HTML
    text = clean_html(text)

    # Step 2: Normalize whitespace
    text = normalize_whitespace(text)

    # Step 3: Remove boilerplate (optional)
    if remove_boilerplate_text:
        text = remove_boilerplate(text)
        text = normalize_whitespace(text)  # Re-normalize after removal

    # Step 4: Truncate to max tokens
    text = truncate_to_max_tokens(text, max_tokens)

    if not text:
        logger.warning("Text became empty after preprocessing")

    return text


def estimate_token_count(text: str) -> int:
    """
    Estimate the number of tokens in text.
    Uses simple word-based estimation.

    Args:
        text: Text to estimate

    Returns:
        Estimated token count
    """
    if not text:
        return 0

    # Rough estimation: ~1.3 tokens per word for English
    word_count = len(text.split())
    return int(word_count * 1.3)
