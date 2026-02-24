import threading
import requests
import time

# Configuration
URL = "http://localhost:8080/upload"
FILE_PATH = "test.txt"
CONCURRENT_USERS = 50
DURATION_SECONDS = 60

# Create a dummy file if it doesn't exist
with open(FILE_PATH, "w") as f:
    f.write("This is a test file for performance testing.")

def run_user_loop(stop_time):
    while time.time() < stop_time:
        try:
            with open(FILE_PATH, 'rb') as f:
                files = {'file': f}
                response = requests.post(URL, files=files)
                if response.status_code != 200:
                    print(f"Error: {response.status_code}")
        except Exception as e:
            print(f"Exception: {e}")

if __name__ == "__main__":
    print(f"Starting load test: {CONCURRENT_USERS} users for {DURATION_SECONDS} seconds...")
    stop_time = time.time() + DURATION_SECONDS
    threads = []
    
    for i in range(CONCURRENT_USERS):
        t = threading.Thread(target=run_user_loop, args=(stop_time,))
        t.start()
        threads.append(t)
    
    for t in threads:
        t.join()
    
    print("Load test complete.")
