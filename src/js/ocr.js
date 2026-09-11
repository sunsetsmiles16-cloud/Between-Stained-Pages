export const OCRService = {
    async compressImage(file, maxWidth = 1200, quality = 0.8) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = (event) => {
                const img = new Image();
                img.src = event.target.result;
                img.onload = () => {
                    const canvas = document.createElement('canvas');
                    let width = img.width;
                    let height = img.height;

                    if (width > maxWidth) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    }

                    canvas.width = width;
                    canvas.height = height;

                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    resolve(canvas.toDataURL('image/jpeg', quality));
                };
                img.onerror = reject;
            };
            reader.onerror = reject;
        });
    },

    async parseImage(base64Data, mimeType = 'image/jpeg') {
        const res = await fetch('/api/parse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ imageBase64: base64Data, mimeType })
        });
        const parsed = await res.json();
        if (!res.ok) throw new Error(parsed.error || 'Transcription failed');
        return parsed;
    }
};
