"""Tests for the retry decorator."""

import asyncio
import pytest
import time
from unittest.mock import Mock, patch

from utils.retry import retry


class TestRetryDecorator:
    """Test cases for sync and async retry decorator."""

    def test_sync_success_without_retry(self):
        """Test sync function succeeds on first attempt."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        def success_func():
            nonlocal call_count
            call_count += 1
            return "success"

        result = success_func()
        assert result == "success"
        assert call_count == 1

    def test_sync_success_after_retries(self):
        """Test sync function succeeds after retries."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        def failing_then_success():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ValueError("Temporary error")
            return "success"

        result = failing_then_success()
        assert result == "success"
        assert call_count == 3

    def test_sync_failure_after_max_attempts(self):
        """Test sync function raises exception after max attempts."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        def always_fails():
            nonlocal call_count
            call_count += 1
            raise ValueError("Permanent error")

        with pytest.raises(ValueError, match="Permanent error"):
            always_fails()
        assert call_count == 3

    @pytest.mark.asyncio
    async def test_async_success_without_retry(self):
        """Test async function succeeds on first attempt."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        async def async_success():
            nonlocal call_count
            call_count += 1
            return "async_success"

        result = await async_success()
        assert result == "async_success"
        assert call_count == 1

    @pytest.mark.asyncio
    async def test_async_success_after_retries(self):
        """Test async function succeeds after retries."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        async def async_failing_then_success():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ValueError("Temporary async error")
            return "async_success"

        result = await async_failing_then_success()
        assert result == "async_success"
        assert call_count == 3

    @pytest.mark.asyncio
    async def test_async_failure_after_max_attempts(self):
        """Test async function raises exception after max attempts."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01)
        async def async_always_fails():
            nonlocal call_count
            call_count += 1
            raise RuntimeError("Permanent async error")

        with pytest.raises(RuntimeError, match="Permanent async error"):
            await async_always_fails()
        assert call_count == 3

    def test_custom_exception_filtering(self):
        """Test that only specified exceptions trigger retry."""
        call_count = 0

        @retry(max_attempts=3, base_delay=0.01, exceptions=(ValueError,))
        def selective_retry():
            nonlocal call_count
            call_count += 1
            if call_count == 1:
                raise ValueError("Retryable error")
            else:
                raise RuntimeError("Non-retryable error")

        with pytest.raises(RuntimeError, match="Non-retryable error"):
            selective_retry()
        assert call_count == 2

    def test_exponential_backoff_delay(self):
        """Test exponential backoff delay calculation."""
        call_times = []

        @retry(max_attempts=3, base_delay=0.05)
        def track_timing():
            call_times.append(time.time())
            if len(call_times) < 3:
                raise ValueError("Error")
            return "success"

        track_timing()

        # Verify delays are exponential: ~0.05s, ~0.1s
        assert len(call_times) == 3
        delay1 = call_times[1] - call_times[0]
        delay2 = call_times[2] - call_times[1]

        # Allow some tolerance for timing variance
        assert 0.03 < delay1 < 0.15  # Should be ~0.05s
        assert 0.08 < delay2 < 0.25  # Should be ~0.1s
