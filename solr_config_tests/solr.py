from typing import Any, Dict
from requests import Response, get
from time import time, sleep
from datetime import datetime

url: str = " http://localhost:8085/solr/admin/cores?core=alfresco&cores=alfresco&action=summary&wt=json"


def is_still_going() -> bool:
    response: Response = get(url)
    response_body: Dict[str, Any] = response.json()
    return response_body["Summary"]["alfresco"]["Approx transactions remaining"] != 0 or response_body["Summary"]["alfresco"]["Approx change sets remaining"] != 0

def wait_until_solr_is_up() -> None:
    print("Waiting for Solr to be up...")
    while True:
        try:
            is_still_going()
            break
        except:
            sleep(10)

def main() -> None:
    wait_until_solr_is_up()

    if not is_still_going():
        print("Solr is already fully indexed! Waiting for this condition to change...")
        while not is_still_going():
            sleep(10)

    start: float = time()
    print(f"Solr is indexing (start time: {datetime.now()})...")
    while is_still_going():
        sleep(10)
    end: float = time()
    print(f"Solr has finished indexing (end time: {datetime.now()})! It took {end - start} second(s).")
    exit(0)


if __name__ == "__main__":
    main()
