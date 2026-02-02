# Android Dev Checklist

## Token Auth quick test

1. Set a temporary token in the app code before making a request:
   ```kotlin
   ApiClient.setToken("test-token")
   ```
2. Temporarily switch the OkHttp logging level to `HEADERS` in `ApiClient` to see the `Authorization` header.
3. Run the app and trigger a request (e.g., health check). Confirm log output includes:
   ```
   Authorization: Bearer test-token
   ```
4. Remove the temporary token and restore the logging level when done.
