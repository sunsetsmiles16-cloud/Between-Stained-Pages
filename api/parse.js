export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: 'Missing GEMINI_API_KEY in Vercel environment variables.' });
  }

  const { imageBase64, mimeType, text } = req.body;

  const systemPrompt = `You are a vintage culinary archivist. 
Extract or transcribe the recipe into valid raw JSON only. Do not wrap in markdown or backticks. Return strictly raw JSON matching this structure:
{
  "title": "Recipe Title",
  "binder_category": "Desserts" or "Sunday Bakes" or "Mains",
  "description": "Short 1-2 sentence description",
  "prep_time": "15m",
  "cook_time": "30m",
  "difficulty": "Easy",
  "servings": 4,
  "ingredients": [
    { "id": "i1", "name": "Ingredient with measurement", "quantity": 1 }
  ],
  "steps": [
    { "step_number": 1, "title": "Step title", "instruction": "Step instruction" }
  ],
  "secret_note": "Any tips, marginalia, or empty string"
}`;

  let parts = [];
  let requestBody = {};

  if (imageBase64) {
    // Multimodal OCR: Camera card scan
    parts = [
      { text: systemPrompt },
      { inline_data: { mime_type: mimeType || 'image/jpeg', data: imageBase64 } }
    ];
    requestBody = {
      contents: [{ parts }],
      generationConfig: { response_mime_type: "application/json" }
    };
  } else if (text) {
    // Web link or pasted recipe text: Enable Google Search Grounding to fetch the URL contents
    parts = [
      { text: `${systemPrompt}\n\nTarget content or URL:\n${text}` }
    ];
    requestBody = {
      contents: [{ parts }],
      tools: [{ google_search: {} }]
    };
  } else {
    return res.status(400).json({ error: 'No recipe image or text provided' });
  }

  try {
    const response = await fetch(
      `[https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$](https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$){apiKey}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      }
    );

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error?.message || 'Failed to communicate with Gemini API');
    }

    const rawOutput = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!rawOutput) {
      throw new Error('No readable recipe content generated');
    }

    // Clean any markdown backticks or accidental surrounding prose
    let cleanJson = rawOutput.trim();
    if (cleanJson.startsWith('```json')) {
      cleanJson = cleanJson.replace(/^```json\s*/i, '').replace(/\s*```$/, '');
    } else if (cleanJson.startsWith('```')) {
      cleanJson = cleanJson.replace(/^```\s*/i, '').replace(/\s*```$/, '');
    }

    // Isolate the outermost JSON block if needed
    const firstBrace = cleanJson.indexOf('{');
    const lastBrace = cleanJson.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace !== -1) {
      cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
    }

    const recipeJson = JSON.parse(cleanJson);
    return res.status(200).json(recipeJson);
  } catch (error) {
    console.error('API Parse Error:', error);
    return res.status(500).json({ error: error.message || 'Error processing recipe data' });
  }
}
