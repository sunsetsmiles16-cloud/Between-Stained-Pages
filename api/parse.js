
export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: 'Missing API key in Vercel settings.' });
  }

  const { imageBase64, mimeType, text } = req.body;

  let parts = [];
  const systemPrompt = `You are a vintage cookbook archivist. Extract the recipe into valid raw JSON only (no markdown, no backticks):
{
  "title": "Title of Recipe",
  "binder_category": "Desserts",
  "description": "Short 1-2 sentence description",
  "prep_time": "15m",
  "cook_time": "30m",
  "difficulty": "Easy",
  "servings": 4,
  "ingredients": [{ "id": "i1", "name": "Ingredient with unit", "quantity": 1 }],
  "steps": [{ "step_number": 1, "title": "Step title", "instruction": "Step instruction" }],
  "secret_note": "Any marginalia note, or empty string"
}`;

  if (imageBase64) {
    parts = [
      { text: systemPrompt },
      { inline_data: { mime_type: mimeType || 'image/jpeg', data: imageBase64 } }
    ];
  } else if (text) {
    parts = [
      { text: systemPrompt + `\n\nRecipe text:\n${text}` }
    ];
  } else {
    return res.status(400).json({ error: 'No image or text provided' });
  }

  try {
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts }],
        generationConfig: { response_mime_type: "application/json" }
      })
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.error?.message || 'Gemini error');
    }

    const recipeJson = JSON.parse(data.candidates[0].content.parts[0].text);
    return res.status(200).json(recipeJson);
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
}
