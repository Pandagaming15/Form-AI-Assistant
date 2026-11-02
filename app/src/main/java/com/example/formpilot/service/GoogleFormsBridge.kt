package com.example.formpilot.service

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.QuestionType
import org.json.JSONArray
import org.json.JSONObject

class GoogleFormsBridge(
  private val onQuestionsExtracted: (formTitle: String, questions: List<FormQuestion>) -> Unit,
  private val onFillReport: (successCount: Int, failedCount: Int, message: String) -> Unit
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

    // JavaScript to extract questions from Google Forms DOM
    val EXTRACTION_SCRIPT = """
      (function() {
        try {
          var titleEl = document.querySelector('.F9yp7e') || 
                        document.querySelector('.freebirdFormviewerViewHeaderTitle') || 
                        document.querySelector('[role="heading"]') || 
                        document.querySelector('h1');
          var formTitle = titleEl ? titleEl.innerText.trim() : document.title || "Google Form";

          var questions = [];
          // Google Forms question cards
          var containers = document.querySelectorAll('.geS5n, .Qr7Oae, .freebirdFormviewerViewItemsItemItem');
          if (!containers || containers.length === 0) {
            // fallback: check role listitems
            containers = document.querySelectorAll('[role="listitem"]');
          }

          for (var i = 0; i < containers.length; i++) {
            var c = containers[i];
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
            var timeInputs = c.querySelectorAll('input[type="number"], input[type="text"][aria-label*="hour"]');

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
              text: qText,
              type: qType,
              isRequired: isReq,
              options: options,
              domSelector: '.geS5n:nth-of-type(' + (i + 1) + ')'
            });
          }

          var payload = JSON.stringify({
            title: formTitle,
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
                    // Check if already checked
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
            window.FormPilotNative.onFillComplete(filled, missed, "Filled " + filled + " fields. " + (missed > 0 ? missed + " fields need manual check." : "All matching fields filled!"));
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
            id = "q_$i",
            questionIndex = qObj.optInt("index", i),
            questionText = qObj.optString("text", "Question ${i + 1}"),
            questionType = qType.name,
            isRequired = qObj.optBoolean("isRequired", false),
            options = options,
            confidence = ConfidenceLevel.HIGH.name,
            domSelector = qObj.optString("domSelector", "")
          )
        )
      }
      onQuestionsExtracted(title, list)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @JavascriptInterface
  fun onFillComplete(successCount: Int, failedCount: Int, message: String) {
    onFillReport(successCount, failedCount, message)
  }
}
