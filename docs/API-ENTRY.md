# Enter API values without remote typing

The Compose app uses its own data sandbox. A Client ID/account saved in the Java launcher is not copied automatically. Reuse your existing public SIMKL Client ID from [SIMKL developer settings](https://simkl.com/settings/developer/); do not create another app just to change launcher UI. After saving the ID, scan the TV's sign-in QR code and approve on your phone. Never share passwords, client secrets or refresh tokens in chat.

## Integration setup

SIMKL setup is under **Settings → Integrations → SIMKL**. TMDB setup is under **Settings → Integrations → TMDB → Add API credential**; use your own API v3 key or Read Access Token. The shared field masks TMDB input. Save the credential, then enable **Use in Search**. Do not paste the credential into GitHub issues or source files. The paste helper below also works in that field; prefer its hidden prompt/explicit clipboard mode over shell-history arguments.

## Phone keyboard

Open the Google TV app's remote on your phone, pair it with the TV, select the text field on the TV and press OK to open its keyboard. Use your phone keyboard to enter the value. [Google instructions](https://support.google.com/chromecast/answer/11221499?hl=en).

## Computer paste

1. On the TV, open the correct field in Matrix Compose and press OK so its keyboard appears.
2. Copy the value on this computer.
3. From this repository, run:

```powershell
./scripts/enter-tv-text.ps1 -Serial <TV-IP>:5555 -FromClipboard
```

Review the value on the TV and choose Save yourself. The helper never submits the form. It checks the foreground app and active keyboard, refuses shell metacharacters, and does not write dumps or values to files. Clipboard input is read only when explicitly requested. Omit `-FromClipboard` for a hidden local prompt instead. `-PublicText` is intended only for public IDs or dummy tests; do not put secrets in shell command history. Older TVs without select-all support can clear a field manually and use `-Append`.

Wallpapers do not require an API account or credentials. Local ADB must already be connected/authorized; the helper does not enable debugging or open any network service.
