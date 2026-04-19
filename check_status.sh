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
    
    first=true
    for url in $urls; do
        if [ "$first" = true ]; then
            first=false
            continue # skip the default domain
        fi
        
        status=$(curl -sI -A "Mozilla/5.0" -m 5 -w "%{http_code}" -o /dev/null "https://$url/")
        loc=$(curl -sI -A "Mozilla/5.0" -m 5 "https://$url/" | grep -i '^location:' | tr -d '\r')
        
        if [ "$status" = "000" ]; then
            echo "[DEAD] $url (Timeout/Error)"
        elif [[ "$status" =~ ^(2|3|403) ]]; then
            # 403 usually means cloudflare block which means it's alive
            echo "[ALIVE] $url (Status: $status) $loc"
        else
            echo "[ERROR] $url (Status: $status)"
        fi
    done
done
