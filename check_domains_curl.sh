groups=(
    "PornComic18|18porncomic.com 18comic.vip 18kami.com"
    "MimiHentai|mimihentai.com hentaihvn.com"
    "PornComixOnline|porncomix.online porncomix.com r34porn.com"
    "Hentalk|hentalk.pw fakku.cc fakku.net"
    "HentaiForce|hentaiforce.net hentaifc.com fhentai.net"
    "NineNineNineHentai|9hentai.so animeh.to 9hentai.com"
    "AllPornComic|allporncomic.com allporncomics.co"
    "MangaFreak|mangafreak.online mfreak.net"
    "HiveScans|hivetoons.org hivetoon.com hivescans.com"
    "KdtScans|www.silentquill.net silentquill.com"
    "RezoScans|rezoscans.com zeroscans.com"
)

for group in "${groups[@]}"; do
    name="${group%%|*}"
    urls="${group##*|}"
    echo "--- $name ---"
    for url in $urls; do
        html=$(curl -sA "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" -m 10 "https://$url" 2>/dev/null)
        if [[ -z "$html" ]]; then
            echo "[FAIL] $url (No response or timeout)"
            continue
        fi

        # Check for cloudflare or other known challenges
        if echo "$html" | grep -qi 'challenge-error-text\|cloudflare'; then
             echo "[CF] $url (Cloudflare protection active)"
             continue
        fi

        title=$(echo "$html" | grep -io '<title[^>]*>.*</title>' | sed 's/<[^>]*>//g' | head -n 1)
        if [[ -z "$title" ]]; then
            echo "[OK-NO-TITLE] $url"
        else
            echo "[OK] $url -> $title"
        fi
    done
done
