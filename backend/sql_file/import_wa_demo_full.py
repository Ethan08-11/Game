"""
分块导入 backend/sql_file/wa_demo最终版.sql 到 Zeabur MySQL。
整文件一次导入易 Lost connection；按小块导入，失败用 INSERT IGNORE 续传。
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

MYSQL = Path(r"C:\Yzr\Mysql5.7\mysql-5.7.37-winx64\bin\mysql.exe")
HOST = os.environ.get("ZEABUR_MYSQL_HOST", "43.133.220.242")
PORT = os.environ.get("ZEABUR_MYSQL_PORT", "32030")
USER = os.environ.get("ZEABUR_MYSQL_USER", "root")
PASSWORD = os.environ.get("ZEABUR_MYSQL_PASSWORD", "Va54ErBg97tdkNhxviw80Y2L1p6zZA3G")
DATABASE = "wa_demo"

ROOT = Path(__file__).resolve().parent
SRC_CANDIDATES = [
    ROOT / "wa_demo最终版.sql",
    Path(os.environ.get("TEMP", tempfile.gettempdir())) / "wa_demo_final.sql",
]
# 公网连接脆弱，块要小
MAX_CHUNK_BYTES = 64_000
MAX_INSERTS = 40
RETRIES = 5


def log(msg: str) -> None:
    print(msg, flush=True)


def find_src() -> Path:
    for p in SRC_CANDIDATES:
        if p.exists():
            return p
    raise FileNotFoundError("找不到 wa_demo最终版.sql")


def mysql_base() -> list[str]:
    return [
        str(MYSQL),
        "-h", HOST,
        "-P", PORT,
        "-u", USER,
        f"-p{PASSWORD}",
        "--default-character-set=utf8mb4",
        "--max_allowed_packet=512M",
        "--connect-timeout=60",
        "--binary-mode",
        "--net_buffer_length=16384",
    ]


def run_sql(payload: bytes, db: str | None = None) -> subprocess.CompletedProcess:
    cmd = mysql_base()
    if db:
        cmd.append(db)
    return subprocess.run(cmd, input=payload, capture_output=True)


def recreate_db() -> None:
    payload = (
        f"DROP DATABASE IF EXISTS `{DATABASE}`;\n"
        f"CREATE DATABASE `{DATABASE}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;\n"
    ).encode("utf-8")
    proc = run_sql(payload)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        raise SystemExit(f"recreate db failed: {proc.returncode}")


def split_chunks(data: bytes) -> list[bytes]:
    lines = data.splitlines(keepends=True)
    chunks: list[bytes] = []
    buf = bytearray()
    inserts = 0

    def flush() -> None:
        nonlocal buf, inserts
        if buf:
            chunks.append(bytes(buf))
            buf = bytearray()
            inserts = 0

    for line in lines:
        if line.lstrip().startswith(b"DROP TABLE IF EXISTS") and len(buf) > 8_000:
            flush()
        buf.extend(line)
        if line.lstrip().startswith(b"INSERT INTO"):
            inserts += 1
        if inserts >= MAX_INSERTS or len(buf) >= MAX_CHUNK_BYTES:
            flush()
    flush()
    return chunks


def to_ignore(chunk: bytes) -> bytes:
    return re.sub(rb"INSERT INTO ", b"INSERT IGNORE INTO ", chunk)


def import_chunk(idx: int, total: int, chunk: bytes) -> None:
    last_err = b""
    for attempt in range(1, RETRIES + 1):
        body = chunk if attempt == 1 else to_ignore(chunk)
        payload = b"SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n" + body + b"\nSET FOREIGN_KEY_CHECKS=1;\n"
        proc = run_sql(payload, DATABASE)
        if proc.returncode == 0:
            log(f"[{idx}/{total}] ok size={len(chunk)} attempt={attempt}")
            return
        last_err = proc.stderr
        msg = last_err.decode("utf-8", errors="replace").strip().replace("\n", " | ")
        log(f"[{idx}/{total}] fail attempt={attempt}: {msg[:260]}")
        # duplicate after partial success -> retry with IGNORE immediately
        if b"Duplicate entry" in last_err:
            continue
        time.sleep(min(2 * attempt, 10))
    raise SystemExit(f"chunk {idx} failed:\n{last_err.decode('utf-8', errors='replace')}")


def verify() -> None:
    q = (
        "SELECT COUNT(*) AS tables_cnt FROM information_schema.tables WHERE table_schema='wa_demo';\n"
        "SELECT COUNT(*) AS users_cnt FROM wa_demo.users;\n"
        "SELECT COUNT(*) AS cards_cnt FROM wa_demo.cards;\n"
        "SELECT COUNT(*) AS friendships_cnt FROM wa_demo.friendships;\n"
        "SELECT COUNT(*) AS match_cards_cnt FROM wa_demo.match_cards;\n"
        "SELECT COUNT(*) AS match_actions_cnt FROM wa_demo.match_actions;\n"
    ).encode()
    proc = run_sql(q)
    sys.stdout.write(proc.stdout.decode("utf-8", errors="replace"))
    sys.stdout.flush()
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode("utf-8", errors="replace"))
        raise SystemExit(proc.returncode)


def main() -> None:
    resume = "--resume" in sys.argv
    if not MYSQL.exists():
        raise SystemExit(f"mysql client not found: {MYSQL}")
    src = find_src()
    log(f"source={src} size={src.stat().st_size} resume={resume}")
    data = src.read_bytes()
    if not resume:
        log("recreate database...")
        recreate_db()
    else:
        log("resume mode: keep existing wa_demo")
    chunks = split_chunks(data)
    log(f"chunks={len(chunks)}")
    start = 1
    if resume and len(sys.argv) >= 3 and sys.argv[2].isdigit():
        start = int(sys.argv[2])
    for i, chunk in enumerate(chunks, 1):
        if i < start:
            continue
        import_chunk(i, len(chunks), chunk)
    log("verify...")
    verify()
    log("done")


if __name__ == "__main__":
    main()
