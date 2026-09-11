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
Analyze the provided image or text carefully.

CRITICAL INGREDIENTS RULE:
Recipes frequently contain MULTIPLE distinct ingredient subsections (e.g., "For the Crust", "For the Toppings", "For the Filling", "For the Sauce", "Garnish").
You MUST capture EVERY SINGLE ingredient across ALL sections. Do NOT stop after reading the first section.
Assign each ingredient a "section" field matching its subsection header (e.g., "For the High-Protein Crust", "For the Toppings", or "Main" if no subsection exists).

Return strictly valid raw JSON with NO markdown backticks, NO backtick fences, and NO conversational text:
{
  "title": "Recipe Title",
  "binder_category": "Mains",
  "description": "Short description or nutritional overview if present",
  "prep_time": "15m",
  "cook_time": "20m",
  "difficulty": "Easy",
  "servings": 4,
  "ingredients": [
    {
      "id": "i1",
      "section": "For the High-Protein Crust",
      "name": "plain nonfat Greek yogurt",
      "quantity": 1,
      "unit": "cup",
      "note": ""
    },
    {
      "id": "i2",
      "section": "For the Toppings",
      "name": "pizza sauce",
      "quantity": 0.5,
      "unit": "cup",
      "note": ""
    }
  ],
  "steps": [
    {
      "step_number": 1,
      "title": "Prepare the Dough",
      "instruction": "Preheat oven to 450°F. In a bowl, mix Greek yogurt, self-rising flour, and protein powder."
    }
  ],
  "secret_note": "Any serving size notes, calories, or marginalia"
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

  const maxRetries = 3;
  let lastError = null;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
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
        const errorMsg = data.error?.message || `HTTP ${response.status}`;
        if (response.status === 503 || response.status === 429 || errorMsg.includes('high demand')) {
          lastError = new Error(errorMsg);
          await new Promise((resolve) => setTimeout(resolve, 1500 * attempt));
          continue;
        }
        throw new Error(errorMsg);
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
      if (attempt < maxRetries) {
        await new Promise((resolve) => setTimeout(resolve, 1500 * attempt));
      }
    }
  }

  return res.status(500).json({
    error: lastError ? lastError.message : 'Scanner busy. Please try again.'
  });
}
