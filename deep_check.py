import subprocess
import concurrent.futures
import re
import urllib.request
import urllib.error
import urllib.parse

diff_out = subprocess.check_output(["git", "diff"]).decode("utf-8")
removed = re.findall(r"^\-\s+.*configKeyDomain.*", diff_out, re.MULTILINE)
added = re.findall(r"^\+\s+.*configKeyDomain.*", diff_out, re.MULTILINE)

dom_removed = set(re.findall(r"\"([^\"]+)\"", " ".join(removed)))
dom_added = set(re.findall(r"\"([^\"]+)\"", " ".join(added)))
to_check = [d for d in dom_removed if d not in dom_added and d != "nhentai.com" and "." in d]

PARKED_KEYWORDS = [
    "domain is for sale", "buy this domain", "domain has expired", 
    "parked free", "hugedomains.com", "sedo.com", "dan.com", 
    "this domain may be for sale", "is available for purchase",
    "registered at", "domain parking", "get this domain",
    "the domain name", "acquire this domain", "inquire about this domain"
]

def deep_check(domain):
    # Ensure imports are available inside thread scope
    import urllib.request
    import urllib.error
    import urllib.parse
    
    try:
        req = urllib.request.Request(
            f"https://{domain}/", 
            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"}
        )
        with urllib.request.urlopen(req, timeout=10) as r:
            final_url = r.geturl()
            html = r.read().decode('utf-8', errors='ignore').lower()
            
            initial_netloc = urllib.parse.urlparse(f"https://{domain}/").netloc.replace("www.", "")
            final_netloc = urllib.parse.urlparse(final_url).netloc.replace("www.", "")
            
            if initial_netloc != final_netloc:
                if any(broker in final_netloc for broker in ['sedo', 'dan', 'hugedomains', 'namecheap', 'godaddy']):
                    return domain, "PARKED/BROKER Redirect", final_url
                else:
                    return domain, f"REDIRECTS to different site", final_url

            for kw in PARKED_KEYWORDS:
                if kw in html:
                    return domain, f"PARKED (found '{kw}')", final_url
                    
            return domain, "200 OK (Looks legitimate from body)", final_url
    except urllib.error.HTTPError as e:
        if e.code in [403, 503]:
            return domain, f"{e.code} (Likely Cloudflare/Protection)", ""
        return domain, f"HTTP Error {e.code}", ""
    except Exception as e:
        try:
            req = urllib.request.Request(
                f"http://{domain}/", 
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
            )
            with urllib.request.urlopen(req, timeout=10) as r:
                return domain, "200 OK (HTTP Only)", r.geturl()
        except:
            return domain, "Failed/Timeout", ""

print(f"Deep checking {len(to_check)} domains by downloading HTML and following redirects...")
results = []
with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
    futures = {executor.submit(deep_check, d): d for d in to_check}
    for future in concurrent.futures.as_completed(futures):
        results.append(future.result())

for domain, status, final_url in sorted(results):
    info = f"- `{domain}`: {status}"
    if final_url and urllib.parse.urlparse(final_url).netloc != urllib.parse.urlparse(f"https://{domain}/").netloc:
        info += f" (Final URL: {final_url})"
    print(info)
