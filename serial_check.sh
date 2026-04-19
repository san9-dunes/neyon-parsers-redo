#!/bin/bash
DOMAINS=$(git diff | awk "/^-.*configKeyDomain/ {print}" | grep -o "\"\[^\"]*\"" | tr -d "\"" | sort -u)
KEPT=$(git diff | awk "/^+.*configKeyDomain/ {print}" | grep -o "\"\[^\"]*\"" | tr -d "\"" | sort -u)
CHECK_LIST=""
for d in $DOMAINS; do
    if ! echo "$KEPT" | grep -q "^$d$"; then CHECK_LIST="$CHECK_LIST $d"; fi
done
for url in $CHECK_LIST; do
    if [ "$url" = "nhentai.com" ]; then continue; fi
    status=$(curl -sI -A "Mozilla/5.0" -m 5 -w "%{http_code}" -o /dev/null "https://$url/")
    if [[ "$status" =~ ^(2|3|403) ]]; then echo "- \`$url\` (Status: $status)"; fi
done

