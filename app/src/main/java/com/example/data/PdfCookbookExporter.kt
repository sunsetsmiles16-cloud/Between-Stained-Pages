package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfLayoutStyle(val displayName: String, val description: String) {
    HEIRLOOM_KEEPSAKE("Heirloom Keepsake", "Classic vintage typeface with ornamental border frames and warm accents"),
    CLEAN_MODERN("Clean Minimalist", "Modern clean lines, structured columns, and high-contrast typography"),
    RETRO_SPIRAL("Retro 1970s", "Terracotta headers, recipe card styling, and vintage font accents")
}

data class PdfExportConfig(
    val title: String = "My Heirloom Keepsake Cookbook",
    val subtitle: String = "A collection of cherished family recipes preserved with love",
    val author: String = "Between Stained Pages",
    val layoutStyle: PdfLayoutStyle = PdfLayoutStyle.HEIRLOOM_KEEPSAKE,
    val includeCoverPage: Boolean = true,
    val includeTableOfContents: Boolean = true,
    val includeSecretNotes: Boolean = true
)

object PdfCookbookExporter {

    // Page dimensions (Standard A4 in points: 595 x 842)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    /**
     * Generates a multi-page formatted PDF containing the selected recipes.
     */
    suspend fun generateCookbookPdf(
        context: Context,
        recipes: List<Recipe>,
        config: PdfExportConfig = PdfExportConfig()
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (recipes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No recipes selected for PDF export"))
            }

            val pdfDocument = PdfDocument()
            var pageNumber = 1

            // Color palette definition
            val terracottaColor = Color.rgb(180, 83, 56)      // #B45338
            val charcoalColor = Color.rgb(44, 38, 36)          // #2C2624
            val sageColor = Color.rgb(94, 110, 89)             // #5E6E59
            val mutedGray = Color.rgb(112, 103, 98)            // #706762
            val parchmentColor = Color.rgb(250, 247, 240)      // #FAF7F0
            val cardBoxColor = Color.rgb(243, 238, 227)        // #F3EEE3
            val goldAccent = Color.rgb(212, 160, 23)           // #D4A017

            // ----------------------------------------------------
            // 1. COVER PAGE (If enabled)
            // ----------------------------------------------------
            if (config.includeCoverPage) {
                val coverPageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val coverPage = pdfDocument.startPage(coverPageInfo)
                val canvas = coverPage.canvas

                // Background
                val bgPaint = Paint().apply {
                    color = parchmentColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

                // Double border frame
                val borderPaint = Paint().apply {
                    color = terracottaColor
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    isAntiAlias = true
                }
                canvas.drawRect(MARGIN - 10f, MARGIN - 10f, PAGE_WIDTH - MARGIN + 10f, PAGE_HEIGHT - MARGIN + 10f, borderPaint)

                val innerBorderPaint = Paint().apply {
                    color = terracottaColor
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                    isAntiAlias = true
                }
                canvas.drawRect(MARGIN - 5f, MARGIN - 5f, PAGE_WIDTH - MARGIN + 5f, PAGE_HEIGHT - MARGIN + 5f, innerBorderPaint)

                // Decorative Corner Diamonds
                val cornerPaint = Paint().apply {
                    color = goldAccent
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                drawDiamond(canvas, MARGIN - 7.5f, MARGIN - 7.5f, 6f, cornerPaint)
                drawDiamond(canvas, PAGE_WIDTH - MARGIN + 7.5f, MARGIN - 7.5f, 6f, cornerPaint)
                drawDiamond(canvas, MARGIN - 7.5f, PAGE_HEIGHT - MARGIN + 7.5f, 6f, cornerPaint)
                drawDiamond(canvas, PAGE_WIDTH - MARGIN + 7.5f, PAGE_HEIGHT - MARGIN + 7.5f, 6f, cornerPaint)

                // Header Tag
                val tagPaint = Paint().apply {
                    color = sageColor
                    textSize = 12f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    letterSpacing = 0.15f
                    isAntiAlias = true
                }
                canvas.drawText("• BETWEEN STAINED PAGES HEIRLOOM ARCHIVE •", PAGE_WIDTH / 2f, 160f, tagPaint)

                // Title
                val titlePaint = Paint().apply {
                    color = terracottaColor
                    textSize = 32f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val titleLines = wrapText(config.title, titlePaint, CONTENT_WIDTH - 40f)
                var titleY = 230f
                for (line in titleLines) {
                    canvas.drawText(line, PAGE_WIDTH / 2f, titleY, titlePaint)
                    titleY += 40f
                }

                // Decorative Divider Line
                val linePaint = Paint().apply {
                    color = goldAccent
                    strokeWidth = 1.5f
                }
                canvas.drawLine(PAGE_WIDTH / 2f - 90f, titleY + 10f, PAGE_WIDTH / 2f + 90f, titleY + 10f, linePaint)
                drawDiamond(canvas, PAGE_WIDTH / 2f, titleY + 10f, 4f, cornerPaint)

                // Subtitle
                val subtitlePaint = Paint().apply {
                    color = charcoalColor
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val subLines = wrapText(config.subtitle, subtitlePaint, CONTENT_WIDTH - 60f)
                var subY = titleY + 45f
                for (line in subLines) {
                    canvas.drawText(line, PAGE_WIDTH / 2f, subY, subtitlePaint)
                    subY += 20f
                }

                // Metadata Box (Number of recipes, date, author)
                val metaBoxRect = RectF(PAGE_WIDTH / 2f - 140f, subY + 50f, PAGE_WIDTH / 2f + 140f, subY + 150f)
                val boxBgPaint = Paint().apply {
                    color = cardBoxColor
                    style = Paint.Style.FILL
                }
                val boxStrokePaint = Paint().apply {
                    color = Color.rgb(220, 210, 195)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRoundRect(metaBoxRect, 10f, 10f, boxBgPaint)
                canvas.drawRoundRect(metaBoxRect, 10f, 10f, boxStrokePaint)

                val metaTextPaint = Paint().apply {
                    color = charcoalColor
                    textSize = 11.5f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Total Selected Recipes: ${recipes.size}", PAGE_WIDTH / 2f, metaBoxRect.top + 30f, metaTextPaint)
                canvas.drawText("Compiled on: $dateStr", PAGE_WIDTH / 2f, metaBoxRect.top + 55f, metaTextPaint)
                canvas.drawText("Edition: Digital Keepsake Volume", PAGE_WIDTH / 2f, metaBoxRect.top + 80f, metaTextPaint)

                // Table of Contents Preview on Cover / First page if small
                if (config.includeTableOfContents && recipes.isNotEmpty()) {
                    var tocY = metaBoxRect.bottom + 45f
                    val tocHeadPaint = Paint().apply {
                        color = terracottaColor
                        textSize = 13f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        letterSpacing = 0.1f
                    }
                    canvas.drawText("— TABLE OF CONTENTS —", PAGE_WIDTH / 2f, tocY, tocHeadPaint)
                    tocY += 22f

                    val tocItemPaint = Paint().apply {
                        color = charcoalColor
                        textSize = 10.5f
                        typeface = Typeface.SERIF
                    }
                    val tocPagePaint = Paint().apply {
                        color = mutedGray
                        textSize = 10f
                        typeface = Typeface.SERIF
                        textAlign = Paint.Align.RIGHT
                    }

                    val maxTocItems = minOf(recipes.size, 8)
                    for (i in 0 until maxTocItems) {
                        val r = recipes[i]
                        val rPageNum = (if (config.includeCoverPage) 2 else 1) + i
                        val itemText = "${i + 1}.  ${r.title}"
                        canvas.drawText(itemText, MARGIN + 40f, tocY, tocItemPaint)
                        canvas.drawText("Page $rPageNum", PAGE_WIDTH - MARGIN - 40f, tocY, tocPagePaint)
                        tocY += 17f
                    }
                    if (recipes.size > maxTocItems) {
                        val morePaint = Paint().apply {
                            color = mutedGray
                            textSize = 9.5f
                            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("... and ${recipes.size - maxTocItems} additional heirloom recipes", PAGE_WIDTH / 2f, tocY + 6f, morePaint)
                    }
                }

                // Footer
                val footerPaint = Paint().apply {
                    color = mutedGray
                    textSize = 9.5f
                    typeface = Typeface.SERIF
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Between Stained Pages • Preserving Culinary Legacies & Cursive Notes", PAGE_WIDTH / 2f, PAGE_HEIGHT - 30f, footerPaint)

                pdfDocument.finishPage(coverPage)
                pageNumber++
            }

            // ----------------------------------------------------
            // 2. RECIPE PAGES
            // ----------------------------------------------------
            val totalPagesEstimate = (if (config.includeCoverPage) 1 else 0) + recipes.size

            for ((index, recipe) in recipes.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                val pageBg = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), pageBg)

                // Page Header with Binder Name
                val pageHeaderPaint = Paint().apply {
                    color = mutedGray
                    textSize = 9f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }
                val headerRightPaint = Paint().apply {
                    color = terracottaColor
                    textSize = 9f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.drawText(config.title.uppercase(), MARGIN, 32f, pageHeaderPaint)
                canvas.drawText("RECIPE #${index + 1}", PAGE_WIDTH - MARGIN, 32f, headerRightPaint)

                // Header Divider Line
                val headerLinePaint = Paint().apply {
                    color = Color.rgb(230, 224, 215)
                    strokeWidth = 1f
                }
                canvas.drawLine(MARGIN, 38f, PAGE_WIDTH - MARGIN, 38f, headerLinePaint)

                var currentY = 68f

                // Recipe Title (Serif, Terracotta)
                val recipeTitlePaint = Paint().apply {
                    color = terracottaColor
                    textSize = 22f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                val titleLines = wrapText(recipe.title, recipeTitlePaint, CONTENT_WIDTH)
                for (line in titleLines) {
                    canvas.drawText(line, MARGIN, currentY, recipeTitlePaint)
                    currentY += 26f
                }

                // Source & Heritage Subtitle
                currentY += 2f
                val heritagePaint = Paint().apply {
                    color = charcoalColor
                    textSize = 11f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    isAntiAlias = true
                }
                val sourceText = "Heritage: ${recipe.source} (${recipe.binderCategory})"
                canvas.drawText(sourceText, MARGIN, currentY, heritagePaint)
                currentY += 16f

                // Metadata Pill Bar (Servings, Prep, Cook Time, Difficulty)
                val metaBarRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 30f)
                val metaBarBg = Paint().apply {
                    color = cardBoxColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(metaBarRect, 6f, 6f, metaBarBg)

                val metaPillTextPaint = Paint().apply {
                    color = charcoalColor
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val metaString = "Servings: ${recipe.servings}   •   Prep: ${recipe.prepTime}   •   Cook: ${recipe.cookTime}   •   Level: ${recipe.difficulty}"
                canvas.drawText(metaString, MARGIN + 14f, currentY + 19f, metaPillTextPaint)
                currentY += 46f

                // Section Headers Paint
                val sectionHeaderPaint = Paint().apply {
                    color = terracottaColor
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    letterSpacing = 0.08f
                    isAntiAlias = true
                }
                val sectionUnderline = Paint().apply {
                    color = terracottaColor
                    strokeWidth = 1.2f
                }

                // 2-Column Layout or Structured Layout
                // Left: Ingredients (Width: 200), Right: Directions & Notes (Width: CONTENT_WIDTH - 220)
                val col1Width = 190f
                val col2X = MARGIN + col1Width + 24f
                val col2Width = PAGE_WIDTH - MARGIN - col2X

                // ----------------
                // Left Column: INGREDIENTS
                // ----------------
                var ingY = currentY
                canvas.drawText("INGREDIENTS", MARGIN, ingY, sectionHeaderPaint)
                canvas.drawLine(MARGIN, ingY + 4f, MARGIN + 110f, ingY + 4f, sectionUnderline)
                ingY += 20f

                val ingTextPaint = Paint().apply {
                    color = charcoalColor
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }
                val checkPaint = Paint().apply {
                    color = sageColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                for (ing in recipe.ingredients) {
                    // Draw checkbox square
                    canvas.drawRect(MARGIN, ingY - 8f, MARGIN + 7f, ingY - 1f, checkPaint)

                    val wrapped = wrapText(ing.name, ingTextPaint, col1Width - 16f)
                    for (wLine in wrapped) {
                        canvas.drawText(wLine, MARGIN + 14f, ingY, ingTextPaint)
                        ingY += 14f
                    }
                    ingY += 4f
                }

                // ----------------
                // Right Column: DIRECTIONS & STEPS
                // ----------------
                var stepY = currentY
                canvas.drawText("DIRECTIONS & METHOD", col2X, stepY, sectionHeaderPaint)
                canvas.drawLine(col2X, stepY + 4f, col2X + 160f, stepY + 4f, sectionUnderline)
                stepY += 20f

                val stepNumPaint = Paint().apply {
                    color = terracottaColor
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                val stepTitlePaint = Paint().apply {
                    color = charcoalColor
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }
                val stepDescPaint = Paint().apply {
                    color = charcoalColor
                    textSize = 9.8f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    isAntiAlias = true
                }

                for ((stepIndex, step) in recipe.steps.withIndex()) {
                    // Step Number & Title
                    canvas.drawText("${stepIndex + 1}.", col2X, stepY, stepNumPaint)
                    canvas.drawText(step.title, col2X + 16f, stepY, stepTitlePaint)
                    stepY += 15f

                    // Step Description
                    val stepLines = wrapText(step.instruction, stepDescPaint, col2Width)
                    for (sLine in stepLines) {
                        canvas.drawText(sLine, col2X + 16f, stepY, stepDescPaint)
                        stepY += 13.5f
                    }
                    stepY += 8f
                }

                // ----------------
                // Secret Heirloom Note / Baker's Tip Box (Bottom area)
                // ----------------
                val lowestY = maxOf(ingY, stepY) + 12f
                val noteText = recipe.secretNote ?: "Pro Tip: Let rest for 10 minutes before slicing to allow aromas and juices to set."

                if (config.includeSecretNotes && lowestY < PAGE_HEIGHT - 130f) {
                    val noteBoxTop = lowestY
                    val noteBoxHeight = 65f
                    val noteBoxRect = RectF(MARGIN, noteBoxTop, PAGE_WIDTH - MARGIN, noteBoxTop + noteBoxHeight)

                    val noteBoxBg = Paint().apply {
                        color = Color.rgb(254, 250, 242)
                        style = Paint.Style.FILL
                    }
                    val noteBoxBorder = Paint().apply {
                        color = goldAccent
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                        pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
                    }
                    canvas.drawRoundRect(noteBoxRect, 8f, 8f, noteBoxBg)
                    canvas.drawRoundRect(noteBoxRect, 8f, 8f, noteBoxBorder)

                    val noteHeaderPaint = Paint().apply {
                        color = terracottaColor
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    }
                    canvas.drawText("✦ Heirloom Baker's Note / Cursive Annotation:", MARGIN + 14f, noteBoxTop + 18f, noteHeaderPaint)

                    val noteBodyPaint = Paint().apply {
                        color = charcoalColor
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    }
                    val wrappedNotes = wrapText(noteText, noteBodyPaint, CONTENT_WIDTH - 28f)
                    var noteTextY = noteBoxTop + 34f
                    for (nLine in wrappedNotes.take(2)) {
                        canvas.drawText(nLine, MARGIN + 14f, noteTextY, noteBodyPaint)
                        noteTextY += 13f
                    }
                }

                // Page Footer
                val footerLinePaint = Paint().apply {
                    color = Color.rgb(230, 224, 215)
                    strokeWidth = 0.8f
                }
                canvas.drawLine(MARGIN, PAGE_HEIGHT - 38f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 38f, footerLinePaint)

                val footerLeftPaint = Paint().apply {
                    color = mutedGray
                    textSize = 8.5f
                    typeface = Typeface.SERIF
                }
                val footerRightPaint = Paint().apply {
                    color = mutedGray
                    textSize = 8.5f
                    typeface = Typeface.SERIF
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("Printed with Between Stained Pages • Handcrafted Cookbook Archive", MARGIN, PAGE_HEIGHT - 24f, footerLeftPaint)
                canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 24f, footerRightPaint)

                pdfDocument.finishPage(page)
                pageNumber++
            }

            // Save PDF to App Documents / Cache
            val exportDir = File(context.cacheDir, "pdf_cookbooks").apply {
                if (!exists()) mkdirs()
            }
            val sanitizedTitle = config.title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val pdfFile = File(exportDir, "${sanitizedTitle}_${timestamp}.pdf")

            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share or open the exported PDF file using Android Intent and FileProvider.
     */
    fun shareOrPrintPdf(context: Context, file: File, title: String = "Exported Cookbook PDF") {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Here is your printable heirloom cookbook compiled with Between Stained Pages.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Print or Share Keepsake Cookbook PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share menu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Directly opens the PDF in any installed PDF reader application.
     */
    fun viewPdf(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            // Fall back to share intent if no default viewer
            shareOrPrintPdf(context, file)
        }
    }

    // Helper: Draws a diamond shape on canvas
    private fun drawDiamond(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx + size, cy)
            lineTo(cx, cy + size)
            lineTo(cx - size, cy)
            close()
        }
        canvas.drawPath(path, paint)
    }

    // Helper: Wraps text to fit within a maxWidth in points
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
