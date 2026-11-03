package com.example.formpilot.service

import android.webkit.JavascriptInterface
import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormPageState
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.QuestionType
import org.json.JSONArray
import org.json.JSONObject

class GoogleFormsBridge(
  private val onQuestionsExtracted: (formTitle: String, pageState: FormPageState, questions: List<FormQuestion>) -> Unit,
  private val onFillReport: (successCount: Int, failedCount: Int, message: String) -> Unit,
  private val onNavigationResult: (action: String, success: Boolean, message: String) -> Unit
) {

  companion object {
    const val JS_INTERFACE_NAME = "FormPilotNative"

    fun isValidGoogleFormUrl(url: String): Boolean {
      val trimmed = url.trim()
      return (trimmed.startsWith("https://docs.google.com/forms/") ||
              trimmed.startsWith("https://forms.gle/") ||
              trimmed.startsWith("http://docs.google.com/forms/") ||
              trimmed.startsWith("http://forms.gle/"))
    }

    fun normalizeFormUrl(url: String): String {
      var trimmed = url.trim()
      if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        trimmed = "https://$trimmed"
      }
      return trimmed
    }

    // JavaScript to extract questions and detect active page/section from Google Forms DOM
    val EXTRACTION_SCRIPT = """
      (function() {
        try {
          var titleEl = document.querySelector('.F9yp7e') || 
                        document.querySelector('.freebirdFormviewerViewHeaderTitle') || 
                        document.querySelector('[role="heading"]') || 
                        document.querySelector('h1');
          var formTitle = titleEl ? titleEl.innerText.trim() : document.title || "Google Form";

          // Section title if present
          var secEl = document.querySelector('.v4yCDe, .freebirdFormviewerViewHeaderHeader, .D1wxyf, .JH9Gec');
          var sectionTitle = (secEl && secEl !== titleEl) ? secEl.innerText.trim() : "";

          // Determine page information
          var currentPage = 1;
          var totalPages = 1;
          var pageIndicator = "";

          // 1. Check progress bar
          var progressEl = document.querySelector('[role="progressbar"]');
          if (progressEl) {
            var valText = progressEl.getAttribute('aria-valuetext') || '';
            var valNow = parseInt(progressEl.getAttribute('aria-valuenow') || '0', 10);
            if (valText) pageIndicator = valText;
          }

          // 2. Scan text nodes for "Page X of Y" or "X of Y"
          var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
          var pageRegex = /(?:page|halaman|página|seite)\s*(\d+)\s*(?:of|dari|de|von|\/)\s*(\d+)/i;
          var shortRegex = /\b(\d+)\s*(?:of|dari|\/)\s*(\d+)\b/i;
          var node;
          while ((node = walker.nextNode())) {
            var txt = (node.nodeValue || "").trim();
            var m = pageRegex.exec(txt) || shortRegex.exec(txt);
            if (m) {
              currentPage = parseInt(m[1], 10);
              totalPages = parseInt(m[2], 10);
              pageIndicator = m[0];
              break;
            }
          }

          // 3. Scan navigation buttons
          var hasNext = false;
          var hasPrev = false;
          var hasSubmit = false;

          var buttons = document.querySelectorAll('div[role="button"], button, span[role="button"]');
          for (var b = 0; b < buttons.length; b++) {
            var btn = buttons[b];
            if (btn.offsetParent === null) continue; // skip hidden
            var bText = (btn.innerText || btn.getAttribute('aria-label') || '').trim().toLowerCase();
            if (bText.indexOf('clear') !== -1 || bText.indexOf('kosongkan') !== -1) continue;

            if (/^(next|berikutnya|siguiente|suivant|weiter|avanti|próxima|próximo|далее|次へ|下一步|ถัดไป|tiếp theo|kolejny|dalej)/i.test(bText)) {
              hasNext = true;
            } else if (/^(back|kembali|atrás|retour|zurück|indietro|voltar|назад|戻る|上一步|ย้อนกลับ|quay lại|wstecz)/i.test(bText)) {
              hasPrev = true;
            } else if (/^(submit|kirim|enviar|envoyer|senden|invia|отправить|送信|提交|gửi|wyślij)/i.test(bText)) {
              hasSubmit = true;
            }
          }

          // Infer page info if not explicitly written
          if (!pageIndicator) {
            if (hasPrev && hasNext) {
              currentPage = Math.max(currentPage, 2);
              totalPages = Math.max(totalPages, 3);
              pageIndicator = "Page " + currentPage + " (Multi-page)";
            } else if (hasNext && !hasPrev) {
              currentPage = 1;
              totalPages = Math.max(totalPages, 2);
              pageIndicator = "Page 1 of " + (totalPages > 1 ? totalPages : "2+");
            } else if (hasPrev && hasSubmit) {
              currentPage = Math.max(currentPage, 2);
              totalPages = Math.max(totalPages, currentPage);
              pageIndicator = "Page " + currentPage + " of " + totalPages + " (Final Page)";
            } else {
              pageIndicator = "Page 1 of 1";
            }
          }

          var questions = [];
          // Google Forms question cards
          var containers = document.querySelectorAll('.geS5n, .Qr7Oae, .freebirdFormviewerViewItemsItemItem');
          if (!containers || containers.length === 0) {
            containers = document.querySelectorAll('[role="listitem"]');
          }

          for (var i = 0; i < containers.length; i++) {
            var c = containers[i];
            if (c.offsetParent === null) continue; // Skip hidden elements

            var promptEl = c.querySelector('.M7eMe') || 
                           c.querySelector('.freebirdFormviewerViewItemsItemItemTitle') || 
                           c.querySelector('[role="heading"]');
            if (!promptEl) continue;

            var qText = promptEl.innerText.trim();
            if (!qText) continue;

            var isReq = c.querySelector('.v5Duua') !== null || 
                        c.querySelector('.freebirdFormviewerViewItemsItemRequiredAsterisk') !== null || 
                        promptEl.innerText.indexOf('*') !== -1;
            
            // Clean asterisk from text
            qText = qText.replace(/\s*\*+\s*${'$'}/, '').trim();

            var qType = "SHORT_ANSWER";
            var options = [];

            var radioBtns = c.querySelectorAll('div[role="radio"]');
            var checkboxes = c.querySelectorAll('div[role="checkbox"]');
            var textarea = c.querySelector('textarea');
            var input = c.querySelector('input[type="text"], input:not([type])');
            var listbox = c.querySelector('div[role="listbox"]');
            var dateInput = c.querySelector('input[type="date"]');

            if (radioBtns && radioBtns.length > 0) {
              qType = "MULTIPLE_CHOICE";
              for (var r = 0; r < radioBtns.length; r++) {
                var label = radioBtns[r].getAttribute('aria-label') || 
                            radioBtns[r].getAttribute('data-value') || 
                            radioBtns[r].innerText.trim();
                if (label && options.indexOf(label) === -1) {
                  options.push(label);
                }
              }
            } else if (checkboxes && checkboxes.length > 0) {
              qType = "CHECKBOXES";
              for (var cb = 0; cb < checkboxes.length; cb++) {
                var cbLabel = checkboxes[cb].getAttribute('aria-label') || 
                              checkboxes[cb].getAttribute('data-value') || 
                              checkboxes[cb].innerText.trim();
                if (cbLabel && options.indexOf(cbLabel) === -1) {
                  options.push(cbLabel);
                }
              }
            } else if (listbox) {
              qType = "DROPDOWN";
              var listOptions = c.querySelectorAll('div[role="option"]');
              for (var opt = 0; opt < listOptions.length; opt++) {
                var oText = listOptions[opt].innerText.trim();
                if (oText && options.indexOf(oText) === -1) options.push(oText);
              }
            } else if (textarea) {
              qType = "PARAGRAPH";
            } else if (dateInput) {
              qType = "DATE";
            } else if (input) {
              qType = "SHORT_ANSWER";
            }

            questions.push({
              index: i,
              pageIndex: currentPage,
              text: qText,
              type: qType,
              isRequired: isReq,
              options: options,
              domSelector: '.geS5n:nth-of-type(' + (i + 1) + ')'
            });
          }

          var payload = JSON.stringify({
            title: formTitle,
            sectionTitle: sectionTitle,
            currentPage: currentPage,
            totalPages: totalPages,
            pageIndicator: pageIndicator,
            hasNextPage: hasNext,
            hasPrevPage: hasPrev,
            isSubmitPage: hasSubmit,
            questions: questions
          });

          if (window.FormPilotNative && window.FormPilotNative.onExtracted) {
            window.FormPilotNative.onExtracted(payload);
          }
        } catch (e) {
          console.error("Form extraction error", e);
        }
      })();
    """.trimIndent()

    // Persistent watcher script that auto-detects page changes and DOM updates in Google Forms
    val WATCHER_SCRIPT = """
      (function() {
        if (window._formPilotWatcherActive) return;
        window._formPilotWatcherActive = true;

        var lastStateFingerprint = "";
        var debounceTimer = null;

        function runExtractionCheck() {
          try {
            var containers = document.querySelectorAll('.geS5n, .Qr7Oae, .freebirdFormviewerViewItemsItemItem, [role="listitem"]');
            var firstText = containers.length > 0 ? (containers[0].innerText || "").slice(0, 80) : "";
            var lastText = containers.length > 0 ? (containers[containers.length - 1].innerText || "").slice(0, 80) : "";
            var footer = document.querySelector('.lRwqEb, .Th4ICd, .mG61Ab');
            var footerText = footer ? (footer.innerText || "").slice(0, 80) : "";

            var fingerprint = containers.length + "|" + firstText + "|" + lastText + "|" + footerText;
            if (fingerprint !== lastStateFingerprint && containers.length > 0) {
              lastStateFingerprint = fingerprint;
              $EXTRACTION_SCRIPT
            }
          } catch(err) {
            console.error("FormPilot watcher error", err);
          }
        }

        var observer = new MutationObserver(function() {
          clearTimeout(debounceTimer);
          debounceTimer = setTimeout(runExtractionCheck, 400);
        });

        observer.observe(document.body, { childList: true, subtree: true, characterData: false });
        // Initial run
        setTimeout(runExtractionCheck, 200);
      })();
    """.trimIndent()

    // Script to find and click the Next button on a Google Form
    val NEXT_PAGE_SCRIPT = """
      (function() {
        try {
          function findNextButton() {
            var buttons = document.querySelectorAll('div[role="button"], button, span[role="button"]');
            for (var i = 0; i < buttons.length; i++) {
              var btn = buttons[i];
              if (btn.offsetParent === null) continue;
              var text = (btn.innerText || btn.getAttribute('aria-label') || '').trim().toLowerCase();
              if (/^(next|berikutnya|siguiente|suivant|weiter|avanti|próxima|próximo|далее|次へ|下一步|ถัดไป|tiếp theo|kolejny|dalej)/i.test(text)) {
                return btn;
              }
            }
            // Fallback: navigation container
            var footer = document.querySelector('.lRwqEb, .Th4ICd, .mG61Ab');
            if (footer) {
              var fBtns = footer.querySelectorAll('div[role="button"], button');
              for (var j = 0; j < fBtns.length; j++) {
                var t = (fBtns[j].innerText || '').trim().toLowerCase();
                if (!/back|kembali|atrás|clear|borrar|kosongkan/i.test(t) && fBtns[j].offsetParent !== null) {
                  return fBtns[j];
                }
              }
            }
            return null;
          }

          var nextBtn = findNextButton();
          if (!nextBtn) {
            if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
              window.FormPilotNative.onNavigationResult("next", false, "Next button not found on this page.");
            }
            return;
          }

          nextBtn.scrollIntoView({ behavior: 'smooth', block: 'center' });
          nextBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
          nextBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
          nextBtn.click();

          // Wait 600ms and check for validation errors
          setTimeout(function() {
            var errors = document.querySelectorAll('.RHiDh, [role="alert"], .freebirdFormviewerViewItemsItemErrorMessage');
            var hasVisibleError = false;
            for (var e = 0; e < errors.length; e++) {
              if (errors[e].offsetParent !== null && (errors[e].innerText || '').trim().length > 0) {
                hasVisibleError = true;
                break;
              }
            }
            if (hasVisibleError) {
              if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
                window.FormPilotNative.onNavigationResult("next", false, "Cannot advance: Some required fields on this page need answers.");
              }
            } else {
              if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
                window.FormPilotNative.onNavigationResult("next", true, "Advancing to next page...");
              }
              // Trigger immediate re-extraction
              setTimeout(function() { $EXTRACTION_SCRIPT }, 500);
            }
          }, 600);
        } catch (e) {
          if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
            window.FormPilotNative.onNavigationResult("next", false, "Error clicking Next: " + e.message);
          }
        }
      })();
    """.trimIndent()

    // Script to find and click the Back button on a Google Form
    val PREV_PAGE_SCRIPT = """
      (function() {
        try {
          function findBackButton() {
            var buttons = document.querySelectorAll('div[role="button"], button, span[role="button"]');
            for (var i = 0; i < buttons.length; i++) {
              var btn = buttons[i];
              if (btn.offsetParent === null) continue;
              var text = (btn.innerText || btn.getAttribute('aria-label') || '').trim().toLowerCase();
              if (/^(back|kembali|atrás|retour|zurück|indietro|voltar|назад|戻る|上一步|ย้อนกลับ|quay lại|wstecz)/i.test(text)) {
                return btn;
              }
            }
            var footer = document.querySelector('.lRwqEb, .Th4ICd, .mG61Ab');
            if (footer) {
              var fBtns = footer.querySelectorAll('div[role="button"], button');
              if (fBtns.length >= 2 && fBtns[0].offsetParent !== null) {
                return fBtns[0];
              }
            }
            return null;
          }

          var backBtn = findBackButton();
          if (!backBtn) {
            if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
              window.FormPilotNative.onNavigationResult("prev", false, "Previous/Back button not found on this page.");
            }
            return;
          }

          backBtn.scrollIntoView({ behavior: 'smooth', block: 'center' });
          backBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
          backBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
          backBtn.click();

          if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
            window.FormPilotNative.onNavigationResult("prev", true, "Returning to previous page...");
          }
          setTimeout(function() { $EXTRACTION_SCRIPT }, 500);
        } catch (e) {
          if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
            window.FormPilotNative.onNavigationResult("prev", false, "Error clicking Back: " + e.message);
          }
        }
      })();
    """.trimIndent()

    // Script to find and click the Submit button on a Google Form
    val SUBMIT_FORM_SCRIPT = """
      (function() {
        try {
          function findSubmitButton() {
            var buttons = document.querySelectorAll('div[role="button"], button, span[role="button"]');
            for (var i = 0; i < buttons.length; i++) {
              var btn = buttons[i];
              if (btn.offsetParent === null) continue;
              var text = (btn.innerText || btn.getAttribute('aria-label') || '').trim().toLowerCase();
              if (/^(submit|kirim|enviar|envoyer|senden|invia|отправить|送信|提交|gửi|wyślij)/i.test(text)) {
                return btn;
              }
            }
            return null;
          }

          var submitBtn = findSubmitButton();
          if (!submitBtn) {
            if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
              window.FormPilotNative.onNavigationResult("submit", false, "Submit button not found on this page.");
            }
            return;
          }

          submitBtn.scrollIntoView({ behavior: 'smooth', block: 'center' });
          submitBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
          submitBtn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
          submitBtn.click();

          if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
            window.FormPilotNative.onNavigationResult("submit", true, "Form submitted!");
          }
        } catch (e) {
          if (window.FormPilotNative && window.FormPilotNative.onNavigationResult) {
            window.FormPilotNative.onNavigationResult("submit", false, "Error submitting: " + e.message);
          }
        }
      })();
    """.trimIndent()

    // Generate auto-fill script for a given list of questions and answers
    fun buildFillScript(questions: List<FormQuestion>): String {
      val questionsJson = questions.joinToString(separator = ",", prefix = "[", postfix = "]") { q ->
        val choicesJson = q.selectedChoices.joinToString(separator = ",", prefix = "[", postfix = "]") { c ->
          "\"" + c.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
        }
        val safeText = q.questionText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val safeAnswer = q.suggestedAnswer.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        """{"index":${q.questionIndex},"text":"$safeText","type":"${q.questionType}","answer":"$safeAnswer","choices":$choicesJson}"""
      }

      return """
        (function() {
          var questionsToFill = $questionsJson;
          var filled = 0;
          var missed = 0;
          var details = [];

          var containers = document.querySelectorAll('.geS5n, .Qr7Oae, .freebirdFormviewerViewItemsItemItem, [role="listitem"]');

          for (var i = 0; i < questionsToFill.length; i++) {
            var item = questionsToFill[i];
            var container = (item.index < containers.length) ? containers[item.index] : null;

            // If not found by index, find container matching question text
            if (!container) {
              for (var cIdx = 0; cIdx < containers.length; cIdx++) {
                if (containers[cIdx].innerText && containers[cIdx].innerText.indexOf(item.text) !== -1) {
                  container = containers[cIdx];
                  break;
                }
              }
            }

            if (!container) {
              missed++;
              details.push("Card not found for: " + item.text);
              continue;
            }

            var type = item.type;
            var answer = item.answer || "";
            var choices = item.choices || [];
            if (choices.length === 0 && answer) {
              choices = [answer];
            }

            var success = false;

            if (type === "SHORT_ANSWER") {
              var input = container.querySelector('input[type="text"], input:not([type])');
              if (input && answer) {
                input.value = answer;
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                success = true;
              }
            } else if (type === "PARAGRAPH") {
              var ta = container.querySelector('textarea');
              if (ta && answer) {
                ta.value = answer;
                ta.dispatchEvent(new Event('input', { bubbles: true }));
                ta.dispatchEvent(new Event('change', { bubbles: true }));
                success = true;
              }
            } else if (type === "MULTIPLE_CHOICE" || type === "LINEAR_SCALE" || type === "TRUE_FALSE") {
              var radios = container.querySelectorAll('div[role="radio"]');
              for (var r = 0; r < radios.length; r++) {
                var radio = radios[r];
                var label = radio.getAttribute('aria-label') || radio.getAttribute('data-value') || radio.innerText.trim();
                if (label && (label.toLowerCase() === answer.toLowerCase() || label.indexOf(answer) !== -1)) {
                  radio.click();
                  success = true;
                  break;
                }
              }
            } else if (type === "CHECKBOXES") {
              var cbs = container.querySelectorAll('div[role="checkbox"]');
              for (var cb = 0; cb < cbs.length; cb++) {
                var checkbox = cbs[cb];
                var cbLabel = checkbox.getAttribute('aria-label') || checkbox.getAttribute('data-value') || checkbox.innerText.trim();
                for (var chIdx = 0; chIdx < choices.length; chIdx++) {
                  if (cbLabel && cbLabel.toLowerCase() === choices[chIdx].toLowerCase()) {
                    var isChecked = checkbox.getAttribute('aria-checked') === 'true';
                    if (!isChecked) {
                      checkbox.click();
                    }
                    success = true;
                  }
                }
              }
            }

            if (success) {
              filled++;
            } else {
              missed++;
              details.push("Could not auto-fill: " + item.text);
            }
          }

          if (window.FormPilotNative && window.FormPilotNative.onFillComplete) {
            window.FormPilotNative.onFillComplete(filled, missed, "Filled " + filled + " fields on this page. " + (missed > 0 ? missed + " fields need manual check." : "All fields filled!"));
          }
        })();
      """.trimIndent()
    }
  }

  @JavascriptInterface
  fun onExtracted(payloadJson: String) {
    try {
      val json = JSONObject(payloadJson)
      val title = json.optString("title", "Google Form")
      val sectionTitle = json.optString("sectionTitle", "")
      val currentPage = json.optInt("currentPage", 1)
      val totalPages = json.optInt("totalPages", 1)
      val pageIndicator = json.optString("pageIndicator", "Page $currentPage of $totalPages")
      val hasNextPage = json.optBoolean("hasNextPage", false)
      val hasPrevPage = json.optBoolean("hasPrevPage", false)
      val isSubmitPage = json.optBoolean("isSubmitPage", false)

      val pageState = FormPageState(
        currentPage = currentPage,
        totalPages = totalPages,
        pageIndicatorText = pageIndicator,
        hasNextPage = hasNextPage,
        hasPrevPage = hasPrevPage,
        isSubmitPage = isSubmitPage,
        sectionTitle = sectionTitle
      )

      val questionsArray = json.optJSONArray("questions") ?: JSONArray()
      val list = mutableListOf<FormQuestion>()

      for (i in 0 until questionsArray.length()) {
        val qObj = questionsArray.getJSONObject(i)
        val optionsArr = qObj.optJSONArray("options") ?: JSONArray()
        val options = (0 until optionsArr.length()).map { optionsArr.getString(it) }

        val typeStr = qObj.optString("type", "SHORT_ANSWER")
        val qType = QuestionType.fromRaw(typeStr)

        list.add(
          FormQuestion(
            id = "q_${currentPage}_$i",
            questionIndex = qObj.optInt("index", i),
            pageIndex = qObj.optInt("pageIndex", currentPage),
            questionText = qObj.optString("text", "Question ${i + 1}"),
            questionType = qType.name,
            isRequired = qObj.optBoolean("isRequired", false),
            options = options,
            confidence = ConfidenceLevel.HIGH.name,
            domSelector = qObj.optString("domSelector", "")
          )
        )
      }
      onQuestionsExtracted(title, pageState, list)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @JavascriptInterface
  fun onFillComplete(successCount: Int, failedCount: Int, message: String) {
    onFillReport(successCount, failedCount, message)
  }

  @JavascriptInterface
  fun onNavigationResult(action: String, success: Boolean, message: String) {
    onNavigationResult.invoke(action, success, message)
  }
}
