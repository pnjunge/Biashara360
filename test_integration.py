import sys
import requests
import json
import time

def main():
    api_url = "http://localhost:8081/v1"
    
    # 1. Login to get JWT Token
    print("Attempting login...")
    try:
        login_res = requests.post(f"{api_url}/auth/login", json={
            "email": "admin@biashara360.co.ke",
            "password": "admin123"
        })
        login_res.raise_for_status()
        token = login_res.json().get("data", {}).get("accessToken")
        if not token:
            print("Failed: No token in login response:", login_res.json())
            sys.exit(1)
        print("Login successful! Token acquired.")
    except Exception as e:
        print("Login failed:", e)
        sys.exit(1)

    # 2. Send mock WhatsApp webhook payload to Ktor API
    # Ktor will proxy this to the Node service
    print("Sending mock WhatsApp webhook payload to Ktor...")
    mock_payload = {
        "object": "whatsapp_business_account",
        "entry": [
            {
                "id": "100998877",
                "changes": [
                    {
                        "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                                "display_phone_number": "254700000000",
                                "phone_number_id": "phone-number-id-123"
                            },
                            "contacts": [
                                {
                                    "profile": {
                                        "name": "Jane Doe"
                                    },
                                    "wa_id": "254711223344"
                                }
                            ],
                            "messages": [
                                {
                                    "from": "254711223344",
                                    "id": "wamid.HBgLMjU0NzExMjIzMzQ0FQIAERgSQjU0NjI4QTQ4RjBCNkEzNTE0AA==",
                                    "timestamp": str(int(time.time())),
                                    "text": {
                                        "body": "Hello, I would like to order a package"
                                    },
                                    "type": "text"
                                }
                            ]
                        },
                        "field": "messages"
                    }
                ]
            }
        ]
    }

    import hmac
    import hashlib

    raw_body = json.dumps(mock_payload, separators=(',', ':'))
    signature = hmac.new(b"your_meta_app_secret", raw_body.encode('utf-8'), hashlib.sha256).hexdigest()
    headers_webhook = {
        "x-hub-signature-256": f"sha256={signature}",
        "Content-Type": "application/json"
    }

    try:
        webhook_res = requests.post(f"{api_url}/social/webhook/whatsapp", data=raw_body, headers=headers_webhook)
        webhook_res.raise_for_status()
        print("Webhook processed successfully! Response:", webhook_res.json())
    except Exception as e:
        print("Webhook request failed:", e)
        sys.exit(1)

    # 3. Fetch inbox conversations from Ktor API
    print("Fetching inbox conversations from Ktor...")
    headers = {
        "Authorization": f"Bearer {token}"
    }
    
    # Wait a second for database transaction to persist
    time.sleep(1)
    
    try:
        inbox_res = requests.get(f"{api_url}/social/inbox", headers=headers)
        inbox_res.raise_for_status()
        inbox_data = inbox_res.json()
        print("Inbox response data:")
        print(json.dumps(inbox_data, indent=2))
        
        conversations = inbox_data.get("data", {}).get("data", [])
        found = False
        for c in conversations:
            if c.get("customerName") == "Jane Doe":
                print(f"Success! Found conversation with customerName: {c.get('customerName')}, status: {c.get('status')}")
                found = True
                break
        
        if not found:
            print("Failed: Simulated conversation not found in inbox.")
            sys.exit(1)
            
    except Exception as e:
        print("Fetching inbox failed:", e)
        sys.exit(1)

if __name__ == "__main__":
    main()
