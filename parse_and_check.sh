DOMAINS=$(git diff | awk '/^-.*configKeyDomain/ {print}' | grep -o '"[^"]*"' | tr -d '"' | sort -u)
KEPT=$(git diff | awk '/^\+.*configKeyDomain/ {print}' | grep -o '"[^"]*"' | tr -d '"' | sort -u)

CHECK_LIST=""
for d in $DOMAINS; do
    if ! echo "$KEPT" | grep -q "^$d$"; then
        CHECK_LIST="$CHECK_LIST $d"
    fi
done

check_url() {
    url=$1
    if [ "$url" = "nhentai.com" ]; then return; fi
    status=$(curl -sI -A "Mozilla/5.0" -m 5 -w "%{http_code}" -o /dev/null "https://$url/")
    if [[ "$status" =~ ^(2|3|403) ]]; then
        echo "- \`$url\` (Status: $status)"
    fi
}
export -f check_url
echo "$CHECK_LIST" | tr ' ' '\n' | grep -v '^$' | xargs -n 1 -P 10 -I {} bash -c 'check_url "$@"' _ {} > success_list.txt
cat success_list.txt
