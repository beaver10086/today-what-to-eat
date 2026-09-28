"""Fail a JMeter CSV/JTL run that misses the documented service targets."""
import csv
import math
import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python check_results.py <results.jtl>", file=sys.stderr)
        return 2
    with Path(sys.argv[1]).open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    measured = [row for row in rows if row.get("label") in {"Dish list", "Recommendation"}]
    if not measured:
        print("No dish-list or recommendation samples found", file=sys.stderr)
        return 1
    failed = sum(row.get("success", "").lower() != "true" for row in measured)
    failure_rate = failed / len(measured)
    times = sorted(int(row["elapsed"]) for row in measured)
    p95 = times[max(0, math.ceil(len(times) * 0.95) - 1)]
    print(f"samples={len(measured)} failures={failed} failure_rate={failure_rate:.2%} p95_ms={p95}")
    if len(measured) < 100:
        print("Expected at least 100 measured API calls (30 users x 5 loops x 2 endpoints)", file=sys.stderr)
        return 1
    if failure_rate > 0.01:
        print("Failure rate exceeded 1%", file=sys.stderr)
        return 1
    if p95 > 3000:
        print("p95 response time exceeded 3000 ms", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
