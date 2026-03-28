#!/usr/bin/env bash
# Tạo lại toàn bộ DB demo (xóa restaurant_db cũ + import schema.sql) — CHỈ dùng máy dev.
# Một lần nhập mật khẩu MySQL root.
#
# Cách chạy (từ thư mục gốc repo):
#   bash sql/init-db.sh
# hoặc:
#   chmod +x sql/init-db.sh && ./sql/init-db.sh

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA="$HERE/schema.sql"

if [[ ! -f "$SCHEMA" ]]; then
  echo "Không thấy $SCHEMA" >&2
  exit 1
fi

echo "→ DROP DATABASE restaurant_db (nếu có) + import schema.sql — nhập mật khẩu MySQL root khi được hỏi."
{
  echo "DROP DATABASE IF EXISTS restaurant_db;"
  cat "$SCHEMA"
} | mysql -u root -p --default-character-set=utf8mb4

echo "→ Xong. Kiểm tra: mysql -u root -p -e \"USE restaurant_db; SHOW TABLES;\""
