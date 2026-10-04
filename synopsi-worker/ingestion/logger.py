import logging
import sys
import os
import json
from datetime import datetime, timezone
from logging import LogRecord


class JSONFormatter(logging.Formatter):
    """Custom JSON formatter for structured logging."""

    def format(self, record: LogRecord) -> str:
        """Format log record as JSON with contextual fields."""
        log_data = {
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'level': record.levelname,
            'message': record.getMessage(),
        }

        # Add contextual fields from extra dict if present
        if hasattr(record, 'feed_url') and record.feed_url:
            log_data['feed_url'] = record.feed_url

        if hasattr(record, 'article_count') and record.article_count is not None:
            log_data['article_count'] = record.article_count

        # Add exception info if present
        if record.exc_info:
            log_data['exception'] = self.formatException(record.exc_info)

        return json.dumps(log_data)


def setup_logger(name: str) -> logging.Logger:
    """
    Setup structured JSON logger with LOG_LEVEL support.

    Args:
        name: Logger name (typically __name__)

    Returns:
        Configured logger instance
    """
    # Get log level from environment, default to INFO
    log_level = os.getenv('LOG_LEVEL', 'INFO').upper()

    # Validate log level
    if log_level not in ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']:
        log_level = 'INFO'

    logger = logging.getLogger(name)
    logger.setLevel(getattr(logging, log_level))

    # Remove any existing handlers to avoid duplicates
    logger.handlers.clear()

    # Setup JSON formatter
    json_formatter = JSONFormatter()

    # Add stdout handler with JSON formatter
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setFormatter(json_formatter)
    logger.addHandler(stdout_handler)

    # Add file handler with JSON formatter
    file_handler = logging.FileHandler('ingestion.log')
    file_handler.setFormatter(json_formatter)
    logger.addHandler(file_handler)

    return logger


def log_with_context(logger: logging.Logger, level: str, message: str,
                     feed_url: str = None, article_count: int = None) -> None:
    """
    Log with contextual fields.

    Args:
        logger: Logger instance
        level: Log level ('DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL')
        message: Log message
        feed_url: Optional feed URL context
        article_count: Optional article count context
    """
    extra = {}
    if feed_url:
        extra['feed_url'] = feed_url
    if article_count is not None:
        extra['article_count'] = article_count

    log_method = getattr(logger, level.lower())
    log_method(message, extra=extra)
