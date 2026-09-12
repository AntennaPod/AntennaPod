# Running transcription and ad analysis on Azure OpenAI

AntennaPodSkipper can use your own models hosted on **Azure OpenAI** instead of the
public OpenAI API for the two cloud AI features:

- **Transcription** (audio → text) — uses a *Whisper* deployment
- **Ad analysis** (transcript → ad segments) — uses a *chat model* deployment (e.g. a GPT model)

Local on-device transcription with Vosk remains available and is unaffected; Azure is
simply an additional cloud provider you can pick instead of OpenAI.

## 1. Prerequisites

- An Azure subscription with access to the **Azure OpenAI Service**
  (some models still require requesting access via the Azure portal).
- Either the [Azure portal](https://portal.azure.com) or the
  [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli).

## 2. Create an Azure OpenAI resource

### Using the portal

1. Open the [Azure portal](https://portal.azure.com) and select **Create a resource**.
2. Search for **Azure OpenAI** and select **Create**.
3. Pick a subscription, resource group, region and a resource name
   (e.g. `my-podcast-ai`).
   - **Important:** Whisper is only available in certain regions
     (e.g. *North Central US*, *Sweden Central*, *West Europe*). Check the
     [model availability table](https://learn.microsoft.com/azure/ai-services/openai/concepts/models)
     and pick a region that has both Whisper and your preferred chat model.
4. Choose a pricing tier (Standard S0) and create the resource.

### Using the Azure CLI

```bash
az group create --name podcast-ai-rg --location swedencentral

az cognitiveservices account create \
  --name my-podcast-ai \
  --resource-group podcast-ai-rg \
  --location swedencentral \
  --kind OpenAI \
  --sku S0
```

## 3. Deploy the models

Each model you want to call must be *deployed* on the resource. The **deployment name**
is what the app sends as the "model" in API requests, so note down exactly what you
name your deployments. The fields in the app are free-form, so you can switch either
deployment later without updating the app.

### Using Azure AI Foundry portal

1. Open [Azure AI Foundry](https://ai.azure.com) and select your Azure OpenAI resource.
2. Go to **Deployments** → **Deploy model** → **Deploy base model**.
3. Deploy a **chat model** for ad analysis, e.g. `gpt-5-nano` or `gpt-4o-mini`.
   Give the deployment a name, e.g. `gpt-5-nano`.
4. Deploy **whisper** for transcription. Give the deployment a name, e.g. `whisper`.

### Using the Azure CLI

```bash
# Chat model deployment for ad analysis
az cognitiveservices account deployment create \
  --name my-podcast-ai \
  --resource-group podcast-ai-rg \
  --deployment-name gpt-5-nano \
  --model-name gpt-5-nano \
  --model-version "1" \
  --model-format OpenAI \
  --sku-capacity 1 \
  --sku-name Standard

# Whisper deployment for transcription
az cognitiveservices account deployment create \
  --name my-podcast-ai \
  --resource-group podcast-ai-rg \
  --deployment-name whisper \
  --model-name whisper \
  --model-version "001" \
  --model-format OpenAI \
  --sku-capacity 1 \
  --sku-name Standard
```

(Adjust model names/versions to whatever is available in your region.)

## 4. Get your endpoint and API key

In the Azure portal, open your Azure OpenAI resource and go to
**Resource Management → Keys and Endpoint**, or use the CLI:

```bash
az cognitiveservices account show \
  --name my-podcast-ai --resource-group podcast-ai-rg \
  --query properties.endpoint

az cognitiveservices account keys list \
  --name my-podcast-ai --resource-group podcast-ai-rg
```

The endpoint looks like `https://my-podcast-ai.openai.azure.com`.

## 5. Configure the app

In AntennaPodSkipper, open **Settings → AI & Ad Skipping** and configure the
**Cloud AI provider** section:

| Setting | Value |
|---|---|
| Cloud provider | **Azure OpenAI** |
| Azure endpoint | `https://<your-resource>.openai.azure.com` |
| Azure API key | one of the two keys from *Keys and Endpoint* |
| Ad analysis deployment | the chat deployment name from step 3 (e.g. `gpt-5-nano`) |
| Transcription deployment | the Whisper deployment name from step 3 (e.g. `whisper`) |
| Azure API version | leave the default (`2024-10-21`) unless your deployment requires a newer preview version |

The API key is stored in encrypted preferences on the device.
Both deployment fields can be edited at any time. The selected ad-analysis deployment
must support the Chat Completions API, and the selected transcription deployment must
support the Audio Transcriptions API.

You can still enable **On-Device Transcription** in the same screen; in that case only
the ad analysis step calls your Azure deployment.

## 6. Notes and troubleshooting

- **401 Unauthorized** — wrong API key, or the key belongs to a different resource
  than the endpoint.
- **404 Not Found / DeploymentNotFound** — the deployment name in the app does not
  match a deployment on the resource, or the API version does not support the model.
  Deployment names are case-sensitive.
- **Whisper "model not available"** — your resource is in a region without Whisper;
  create the resource (or a second one) in a supported region.
- **Audio limits** — like the OpenAI API, Azure's Whisper endpoint accepts files up to
  25 MB. The app already splits long episodes into chunks automatically.
- **Costs** — you pay per token (chat) and per transcribed minute (Whisper) according
  to your Azure pricing tier. Consider setting a
  [budget alert](https://learn.microsoft.com/azure/cost-management-billing/costs/cost-mgt-alerts-monitor-usage-spending)
  on the resource group.
