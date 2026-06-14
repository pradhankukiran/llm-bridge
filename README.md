# LLM Bridge

Android app for configuring provider routes, saving API keys locally, and chatting against the active model offering.

## Current Providers

- OpenAI-compatible routes
- Anthropic native `/messages`
- Local Ollama through the Android emulator host alias
- NVIDIA NIM / NVIDIA Integrate as a first-class provider:
  - `moonshotai/kimi-k2.6`
  - `minimaxai/minimax-m3`
  - `z-ai/glm-5.1`
  - `deepseek-ai/deepseek-v4-pro`

## Provider Architecture

The app separates provider logic from chat UI code.

- `ProviderSpec` describes a provider, its default base URL, and wire protocol.
- `ModelOffering` describes one provider-served model flavor, including defaults and capabilities.
- `LlmAdapter` is the provider seam used by the chat client.
- `NvidiaChatAdapter` translates app-level route settings into NVIDIA-specific Chat Completions requests.
- `NvidiaCatalogSyncAdapter` can refresh model IDs from NVIDIA's OpenAI-compatible `/models` endpoint when a saved NVIDIA key is available.

NVIDIA-specific behavior currently handled behind the adapter:

- `202` accepted responses with `/status/{requestId}` polling
- SSE streaming response parsing
- DeepSeek `reasoning_effort`
- Kimi `chat_template_kwargs.thinking`
- MiniMax `chat_template_kwargs.thinking_mode`
- GLM streaming defaults
- OpenAI-style tool definitions and `tool_choice`
- Kimi-compatible image/video URL content parts
- Access metadata for free-prototyping vs unknown pricing

The route editor is organized as:

- Provider: selects OpenAI-compatible, Anthropic, NVIDIA, or local presets.
- Connection: stores route name, base URL, and encrypted API key.
- Offering: selects the model flavor and shows capabilities/access metadata.
- Route: stores inference defaults, reasoning mode, tools, media defaults, and raw JSON overrides.

## Run

Open this directory in Android Studio and run the `app` configuration on an emulator or device.

This export does not currently include a Gradle wrapper, so command-line builds require either adding the wrapper from Android Studio or installing a compatible Gradle version locally.

## Notes

- API keys are entered per provider route inside the app.
- Saved API keys are encrypted with Android Keystore before being written to Room.
- NVIDIA Integrate uses `https://integrate.api.nvidia.com/v1` as an OpenAI-compatible Chat Completions endpoint.
- Provider-specific request tweaks can be entered in the route editor as extra body JSON or extra headers JSON.
