"""Deterministic, rule-based analysis provider.

No network access and no API key required, so this is the default
provider for local development and the only one exercised in CI (see
ADR-0011). It uses simple keyword scoring rather than a real model — good
enough to prove the contract and the end-to-end flow, not a claim of
accurate triage.
"""

import re
from collections import Counter

from app.providers.base import AnalysisProvider
from app.schemas import AnalyseRequest, AnalyseResponse, Priority

DEFAULT_CATEGORY = "Other"

CATEGORY_KEYWORDS: dict[str, set[str]] = {
    "Hardware": {
        "laptop",
        "monitor",
        "screen",
        "keyboard",
        "mouse",
        "printer",
        "device",
        "hardware",
        "battery",
        "charger",
        "docking",
        "webcam",
        "power",
    },
    "Software": {
        "install",
        "license",
        "licence",
        "bug",
        "crash",
        "crashing",
        "application",
        "app",
        "update",
        "software",
        "error",
        "freeze",
        "freezing",
        "version",
    },
    "Network": {
        "vpn",
        "wifi",
        "wi-fi",
        "dns",
        "network",
        "internet",
        "connection",
        "connectivity",
        "ethernet",
        "router",
        "offline",
        "disconnecting",
        "disconnect",
    },
    "Access & Permissions": {
        "password",
        "login",
        "log-in",
        "access",
        "permission",
        "permissions",
        "sso",
        "account",
        "locked",
        "mfa",
        "2fa",
        "credentials",
    },
    "Database": {
        "database",
        "query",
        "sql",
        "db",
        "replication",
        "backup",
        "table",
        "postgres",
        "postgresql",
        "index",
        "migration",
    },
    "Security": {
        "breach",
        "phishing",
        "malware",
        "suspicious",
        "unauthorized",
        "unauthorised",
        "virus",
        "security",
        "ransomware",
        "leak",
        "leaked",
    },
}

CRITICAL_SIGNALS = (
    "down",
    "outage",
    "critical",
    "urgent",
    "emergency",
    "production",
    "breach",
    "unusable",
    "all users",
    "entire team",
    "everyone",
)
HIGH_SIGNALS = (
    "broken",
    "failing",
    "not working",
    "doesn't work",
    "won't",
    "wont",
    "crash",
    "blocked",
    "cannot",
    "can't",
    "cant",
)
MEDIUM_SIGNALS = (
    "slow",
    "intermittent",
    "occasionally",
    "sometimes",
    "minor",
    "degraded",
)

STOPWORDS = {
    "a",
    "about",
    "after",
    "again",
    "all",
    "am",
    "an",
    "and",
    "any",
    "are",
    "as",
    "at",
    "be",
    "been",
    "being",
    "but",
    "by",
    "can",
    "cannot",
    "could",
    "did",
    "do",
    "does",
    "doing",
    "don",
    "down",
    "during",
    "each",
    "few",
    "for",
    "from",
    "further",
    "had",
    "has",
    "have",
    "having",
    "how",
    "i",
    "if",
    "in",
    "into",
    "is",
    "it",
    "its",
    "just",
    "me",
    "more",
    "most",
    "my",
    "no",
    "nor",
    "not",
    "now",
    "of",
    "off",
    "on",
    "once",
    "only",
    "or",
    "other",
    "our",
    "out",
    "over",
    "own",
    "same",
    "she",
    "should",
    "so",
    "some",
    "such",
    "than",
    "that",
    "the",
    "their",
    "them",
    "then",
    "there",
    "these",
    "they",
    "this",
    "those",
    "through",
    "to",
    "too",
    "under",
    "until",
    "up",
    "very",
    "was",
    "we",
    "were",
    "what",
    "when",
    "where",
    "which",
    "while",
    "who",
    "whom",
    "why",
    "will",
    "with",
    "won",
    "would",
    "you",
    "your",
    "it's",
    "i'm",
    "i've",
    "didn't",
    "doesn't",
    "isn't",
    "wasn't",
}

SUGGESTED_STEPS: dict[str, list[str]] = {
    "Hardware": [
        "Confirm the device is powered on and charging cables/ports are seated correctly",
        "Try a different power outlet, cable, or peripheral to isolate the faulty component",
        "Check for a pending firmware/driver update for the affected device",
        "If unresolved, log a hardware replacement/repair request with asset details",
    ],
    "Software": [
        "Restart the application and, if that fails, restart the machine",
        "Check for a pending update or reinstall the affected application",
        "Confirm the license/entitlement is still valid for this user",
        "Collect the exact error message and reproduction steps for engineering follow-up",
    ],
    "Network": [
        "Check the physical/Wi-Fi connection and try reconnecting",
        "Restart the network adapter, VPN client, or router",
        "Confirm DNS resolution and connectivity with a basic reachability test",
        "Escalate to network engineering if the issue affects multiple users",
    ],
    "Access & Permissions": [
        "Verify the account is active and not locked out",
        "Confirm the user is requesting access appropriate to their role",
        "Trigger a password reset or MFA re-enrolment if credentials are the blocker",
        "Escalate to an admin for permission/SSO configuration changes",
    ],
    "Database": [
        "Check recent deployment/migration history for the affected schema",
        "Review slow query logs and current connection pool utilisation",
        "Confirm the most recent backup is valid before attempting any fix",
        "Escalate to the database team if data integrity is at risk",
    ],
    "Security": [
        "Do not attempt to remediate independently — escalate to the security team immediately",
        "Preserve logs and evidence; avoid restarting affected systems prematurely",
        "Identify and isolate affected accounts/devices",
        "Follow the incident response runbook for containment and notification",
    ],
    "Other": [
        "Gather more detail from the reporter to narrow down the affected system",
        "Check whether other users are experiencing the same issue",
        "Assign to the most relevant team based on symptoms once identified",
    ],
}

_TOKEN_PATTERN = re.compile(r"[a-z][a-z'-]+")


def _tokenize(text: str) -> list[str]:
    return _TOKEN_PATTERN.findall(text.lower())


def pick_category(tokens: set[str]) -> str:
    best_category = DEFAULT_CATEGORY
    best_score = 0
    for category, keywords in CATEGORY_KEYWORDS.items():
        score = len(tokens & keywords)
        if score > best_score:
            best_score = score
            best_category = category
    return best_category


def pick_priority(text_lower: str) -> Priority:
    if any(signal in text_lower for signal in CRITICAL_SIGNALS):
        return "CRITICAL"
    if any(signal in text_lower for signal in HIGH_SIGNALS):
        return "HIGH"
    if any(signal in text_lower for signal in MEDIUM_SIGNALS):
        return "MEDIUM"
    return "LOW"


def extract_keywords(tokens: list[str], limit: int = 8) -> list[str]:
    significant = [t for t in tokens if len(t) > 2 and t not in STOPWORDS]
    counts = Counter(significant)
    return [word for word, _count in counts.most_common(limit)]


def build_summary(description: str, max_len: int = 220) -> str:
    first_sentence = re.split(r"(?<=[.!?])\s", description.strip(), maxsplit=1)[0]
    if len(first_sentence) <= max_len:
        return first_sentence
    truncated = first_sentence[:max_len].rsplit(" ", 1)[0]
    return f"{truncated}..."


class MockAnalysisProvider(AnalysisProvider):
    MODEL_NAME = "mock-heuristic-v1"

    async def analyse(self, request: AnalyseRequest) -> AnalyseResponse:
        combined_text = f"{request.title} {request.description}"
        tokens = _tokenize(combined_text)
        category = pick_category(set(tokens))
        return AnalyseResponse(
            suggested_category=category,
            predicted_priority=pick_priority(combined_text.lower()),
            summary=build_summary(request.description),
            keywords=extract_keywords(tokens),
            suggested_steps=SUGGESTED_STEPS.get(category, SUGGESTED_STEPS[DEFAULT_CATEGORY]),
            model_used=self.MODEL_NAME,
        )
