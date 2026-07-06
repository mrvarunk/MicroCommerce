import threading
import requests
import time

# URL of your new stock-service endpoint
URL = "http://localhost:8081/api/v1/stock/decrease"
PRODUCT_ID = 1
QUANTITY = 1


def send_request():
    try:
        response = requests.post(URL, params={"productId": PRODUCT_ID, "quantity": QUANTITY})
        print(f"Status: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"Error: {e}")


# Simulate 50 concurrent users
threads = []
for i in range(50):
    t = threading.Thread(target=send_request)
    threads.append(t)

start_time = time.time()
for t in threads: t.start()
for t in threads: t.join()
print(f"Test finished in {time.time() - start_time:.2f}s")