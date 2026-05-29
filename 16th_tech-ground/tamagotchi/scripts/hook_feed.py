#!/usr/bin/env python3
"""
Claude Code Stop 훅 — 대화가 끝날 때 토큰 사용량을 다마고치 앱으로 전송합니다.

설정 방법 (~/.claude/settings.json):
{
  "hooks": {
    "Stop": [{
      "matcher": "",
      "hooks": [{
        "type": "command",
        "command": "TAMAGOTCHI_API_TOKEN=<your-token> python3 /path/to/hook_feed.py"
      }]
    }]
  }
}
"""

import json
import os
import sys
import urllib.request
import urllib.error

TAMAGOTCHI_URL = os.environ.get("TAMAGOTCHI_URL", "http://localhost:8080/api/tokens")


def read_usage_from_transcript(transcript_path: str) -> tuple[int, int]:
    input_tokens, output_tokens = 0, 0
    try:
        with open(transcript_path, "r") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    entry = json.loads(line)
                    # 직접 usage 필드 or message.usage 필드 탐색
                    usage = entry.get("usage") or entry.get("message", {}).get("usage", {})
                    if usage:
                        input_tokens += usage.get("input_tokens", 0)
                        output_tokens += usage.get("output_tokens", 0)
                except (json.JSONDecodeError, AttributeError):
                    pass
    except (OSError, IOError):
        pass
    return input_tokens, output_tokens


def main():
    api_token = os.environ.get("TAMAGOTCHI_API_TOKEN", "")
    if not api_token:
        sys.exit(0)

    # Claude Code Stop 훅은 stdin으로 세션 정보를 전달
    payload_data = {}
    try:
        raw = sys.stdin.read()
        if raw.strip():
            payload_data = json.loads(raw)
    except (json.JSONDecodeError, OSError):
        pass

    transcript_path = payload_data.get("transcript_path", "")
    input_tokens, output_tokens = read_usage_from_transcript(transcript_path)

    # transcript에서 토큰을 읽지 못한 경우 대화 1회분 기본값 적용
    if input_tokens == 0 and output_tokens == 0:
        output_tokens = 1000

    body = json.dumps({
        "apiToken": api_token,
        "inputTokens": input_tokens,
        "outputTokens": output_tokens,
    }).encode("utf-8")

    try:
        req = urllib.request.Request(
            TAMAGOTCHI_URL,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        urllib.request.urlopen(req, timeout=3)
    except (urllib.error.URLError, OSError):
        pass  # 앱이 꺼져 있으면 조용히 skip


if __name__ == "__main__":
    main()