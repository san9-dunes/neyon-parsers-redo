import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# List of domains grouped by their original parser
groups = {
    "PornComic18": ["18porncomic.com", "18comic.vip", "18kami.com"],
    "MimiHentai": ["mimihentai.com", "hentaihvn.com"],
    "PornComixOnline": ["porncomix.online", "porncomix.com", "r34porn.com"],
    "Hentalk": ["hentalk.pw", "fakku.cc", "fakku.net"],
    "HentaiForce": ["hentaiforce.net", "hentaifc.com", "fhentai.net"],
    "NineNineNineHentai": ["9hentai.so", "animeh.to", "9hentai.com"],
    "AllPornComic": ["allporncomic.com", "allporncomics.co"],
    "MangaFreak": ["mangafreak.online", "mfreak.net"],
    "HiveScans": ["hivetoons.org", "hivetoon.com", "hivescans.com"],
    "KdtScans": ["www.silentquill.net", "silentquill.com"],
    "RezoScans": ["rezoscans.com", "zeroscans.com"]
}

print("Checking domains...")
for group, domains in groups.items():
    print(f"\n--- {group} ---")
    for d in domains:
        url = f"https://{d}"
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'})
            with urllib.request.urlopen(req, context=ctx, timeout=7) as response:
                html = response.read().decode('utf-8', errors='ignore')
                title_match = re.search(r'<title[^>]*>(.*?)</title>', html, re.IGNORECASE | re.DOTALL)
                title = title_match.group(1).strip().replace('\n', ' ') if title_match else 'No Title'
                print(f"[OK] {d} -> Title: {title[:60]}")
        except Exception as e:
            msg = str(e).replace('\n', ' ')
            print(f"[FAIL] {d} -> {type(e).__name__}: {msg[:60]}")
