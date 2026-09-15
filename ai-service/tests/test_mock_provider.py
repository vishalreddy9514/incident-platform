from app.providers.mock import (
    MockAnalysisProvider,
    build_summary,
    extract_keywords,
    pick_category,
    pick_priority,
)
from app.schemas import AnalyseRequest


def test_pick_category_matches_network_keywords():
    assert pick_category({"vpn", "wifi", "disconnecting"}) == "Network"


def test_pick_category_matches_hardware_keywords():
    assert pick_category({"laptop", "screen", "battery"}) == "Hardware"


def test_pick_category_defaults_to_other_with_no_signal():
    assert pick_category({"banana", "unrelated", "word"}) == "Other"


def test_pick_priority_critical_for_outage_language():
    assert pick_priority("production is down for all users") == "CRITICAL"


def test_pick_priority_high_for_broken_language():
    assert pick_priority("the app keeps crashing and won't open") == "HIGH"


def test_pick_priority_medium_for_intermittent_language():
    assert pick_priority("it's a bit slow and intermittent") == "MEDIUM"


def test_pick_priority_low_by_default():
    assert pick_priority("just a small cosmetic question about the UI") == "LOW"


def test_extract_keywords_excludes_stopwords_and_ranks_by_frequency():
    tokens = ["the", "vpn", "is", "down", "and", "vpn", "wont", "connect", "a"]

    keywords = extract_keywords(tokens)

    assert "the" not in keywords
    assert "and" not in keywords
    assert keywords[0] == "vpn"


def test_build_summary_uses_first_sentence():
    description = "The VPN drops constantly. This started yesterday afternoon."

    assert build_summary(description) == "The VPN drops constantly."


def test_build_summary_truncates_long_single_sentence():
    description = "word " * 100

    summary = build_summary(description, max_len=50)

    assert len(summary) <= 53
    assert summary.endswith("...")


async def test_mock_provider_end_to_end():
    provider = MockAnalysisProvider()
    request = AnalyseRequest(
        title="Laptop won't turn on",
        description="My work laptop screen stays black and it won't power on at all, urgent.",
    )

    result = await provider.analyse(request)

    assert result.suggested_category == "Hardware"
    assert result.predicted_priority in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
    assert result.model_used == "mock-heuristic-v1"
    assert result.suggested_steps
