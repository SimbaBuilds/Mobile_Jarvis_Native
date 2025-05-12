## Voice Implementation



Wake word detection is working.  But, when wake word is detected, I want you to implement the following:
1. Wake word detection with picovoice must stop, but listening stays on.  Add logging for this implementation.
2. An OpenAI Whisper client must be initiated.  Once initiated the assistant should say “Sir?” using a deepgram service.  Add logging for this implementation.
3. When I speak, the openAI whisper client should process the audio and display it in the home screen chat interface and send the chat history and user preferences to a third party api which, for now, will be on my local server (IP: 192.168.1.131) but not in this project.  Add logging for this implementation.

Modification: We put the API call in a Typescript file for better extensibility and removal of need to rebuild native code


Testing:
1. Feature specific example: adb logcat -d | grep -i "picovoice" | tail -20 | cat
2. Fetch errors: `adb logcat "*:E" -d | cat`
3. Clear logcat buffer: `adb logcat -c`

More relevant examples for this feature:
1. Feature specific example: adb logcat -d | grep -i "wake word" | tail -20 | cat
2. 1. Feature specific example: adb logcat -d | grep -i "whisper" | tail -20 | cat
3. 1. Feature specific example: adb logcat -d | grep -i "deepgram" | tail -20 | cat
4. 1. Feature specific example: adb logcat -d | grep -i "broadcast" | tail -20 | cat

