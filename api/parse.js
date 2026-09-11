export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: 'Missing GEMINI_API_KEY in Vercel environment variables.' });
  }

  const { imageBase64, mimeType, text } = req.body;

  const systemPrompt = `You are a vintage culinary archivist and precision recipe OCR engine.
Analyze the provided image or text. It may be:
- A handwritten vintage recipe card or notebook page
- A phone screenshot (TikTok overlay, Instagram reel, Notes app, blog screenshot)
- A printed cookbook page

Carefully transcribe all ingredients with measurements and step-by-step directions into standard kitchen units.

Return strictly valid raw JSON with NO markdown backticks, NO formatting ticks, and NO conversational chatter:
{
  "title": "Recipe Title",
  "binder_category": "Sunday Bakes",
  "description": "A warm 1-2 sentence description of the dish",
  "prep_time": "15m",
  "cook_time": "30m",
  "difficulty": "Easy",
  "servings": 4,
  "ingredients": [
    {
      "id": "i1",
      "name": "All-purpose flour",
      "quantity": 2,
      "unit": "cups",
      "note": "sifted"
    }
  ],
  "steps": [
    {
      "step_number": 1,
      "title": "Prep Step",
      "instruction": "Detailed directions for this step."
    }
  ],
  "secret_note": "Any handwritten notes, family tips, or empty string"
}`;

  let parts = [];
  let requestBody = {};

  if (imageBase64) {
    parts = [
      { text: systemPrompt },
      { inline_data: { mime_type: mimeType || 'image/jpeg', data: imageBase64 } }
    ];
    requestBody = {
      contents: [{ parts }],
      generationConfig: { response_mime_type: "application/json" }
    };
  } else if (text) {
    parts = [
      { text: `${systemPrompt}\n\nTarget content or URL:\n${text}` }
    ];
    requestBody = {
      contents: [{ parts }],
      tools: [{ google_search: {} }]
    };
  } else {
    return res.status(400).json({ error: 'No image or text provided' });
  }

  // Model cascade: try primary fast model, fall back if Google is under heavy load
  const models = ['gemini-2.5-flash', 'gemini-1.5-flash'];
  let lastError = null;

  for (const model of models) {
    try {
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        }
      );

      const data = await response.json();

      if (!response.ok) {
        const msg = data.error?.message || `HTTP ${response.status}`;
        // If high demand or capacity error, continue loop to try fallback model
        if (response.status === 503 || response.status === 429 || msg.includes('high demand')) {
          console.warn(`Model ${model} overloaded. Falling back...`);
          lastError = new Error(msg);
          continue;
        }
        throw new Error(msg);
      }

      const rawOutput = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!rawOutput) {
        throw new Error('No recipe content generated');
      }

      let cleanJson = rawOutput.trim();
      if (cleanJson.startsWith('```json')) {
        cleanJson = cleanJson.replace(/^```json\s*/i, '').replace(/\s*```$/, '');
      } else if (cleanJson.startsWith('```')) {
        cleanJson = cleanJson.replace(/^```\s*/i, '').replace(/\s*```$/, '');
      }

      const firstBrace = cleanJson.indexOf('{');
      const lastBrace = cleanJson.lastIndexOf('}');
      if (firstBrace !== -1 && lastBrace !== -1) {
        cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
      }

      const recipeJson = JSON.parse(cleanJson);
      return res.status(200).json(recipeJson);

    } catch (err) {
      lastError = err;
      console.warn(`Error on ${model}:`, err.message);
    }
  }

  return res.status(500).json({
    error: lastError ? lastError.message : 'All vision models are currently under heavy load. Please try again in a few moments.'
  });
}
