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
- A phone screenshot (TikTok recipe overlay, Instagram reel text, Apple Notes, website screenshot)
- A printed cookbook page

Carefully transcribe all ingredients with measurements and step-by-step directions. If information is partially obscured or written in shorthand, interpret it cleanly into conventional kitchen units.

Return strictly valid raw JSON with NO markdown formatting, NO backticks, and NO conversational chatter:
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

  try {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=${apiKey}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      }
    );

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error?.message || 'Gemini API call failed');
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
  } catch (error) {
    console.error('API Parse Error:', error);
    return res.status(500).json({ error: error.message || 'Error processing recipe data' });
  }
}
