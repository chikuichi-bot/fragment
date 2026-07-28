/* ==============================
   ✨ 完全ハイブリッド対応 main.js (iOS / Android両対応)
   ============================== */

(() => {
    let currentQuoteData = {
        text: "",
        title: "",
        author: ""
    };
    
    let activeKeyword = "";
    
    window.setSearchKeyword = function(keyword) {
        activeKeyword = (keyword || "").trim();
    };
    
    window.displayQuoteWithFade = function(text, title, author) {
        currentQuoteData = { text, title, author };
        
        // 🌟 言葉が表示されたら、スワイプガイドを透明にして消す
        const swipeGuide = document.getElementById('swipe-guide');
        if (swipeGuide) {
            swipeGuide.style.opacity = '0';
            // 完全にクリック判定等から消す
            setTimeout(() => { swipeGuide.style.display = 'none'; }, 600);
        }
        
        const quoteEl = document.getElementById('quote-text');
        const sourceEl = document.getElementById('source-area');
        const titleEl = document.getElementById('source-title');
        const authorEl = document.getElementById('source-author');
        const starIcon = document.querySelector('#btn-star svg');
        
        if (starIcon) {
            starIcon.style.fill = "none";
            starIcon.style.color = "";
            starIcon.classList.remove('stocked');
        }
        
        quoteEl.classList.remove('fade-in');
        sourceEl.classList.remove('fade-in');
        
        const keywordParts = activeKeyword ? activeKeyword.toLowerCase().split(' ').filter(p => p.length > 0) : [];
        
        quoteEl.innerHTML = '';
        const words = text.split(' ');
        words.forEach((w, index) => {
            const span = document.createElement('span');
            span.innerText = w;
            span.classList.add('word');
            
            if (keywordParts.length > 0) {
                const lowerW = w.toLowerCase();
                if (keywordParts.some(part => lowerW.includes(part))) {
                    span.classList.add('highlight-word');
                }
            }
            
            quoteEl.appendChild(span);
            if (index < words.length - 1) {
                quoteEl.appendChild(document.createTextNode(' '));
            }
        });
        
        function highlightString(originalString) {
            if (!activeKeyword || !originalString) return originalString;
            const lowerOrig = originalString.toLowerCase();
            const lowerKey = activeKeyword.toLowerCase();
            const matchIndex = lowerOrig.indexOf(lowerKey);
            
            if (matchIndex >= 0) {
                const before = originalString.substring(0, matchIndex);
                const match = originalString.substring(matchIndex, matchIndex + activeKeyword.length);
                const after = originalString.substring(matchIndex + activeKeyword.length);
                return before + '<span class="highlight-word">' + match + '</span>' + after;
            }
            return originalString;
        }
        
        if (title) {
            titleEl.innerHTML = highlightString(`- ${title} -`);
        } else {
            titleEl.innerText = "";
        }
        
        if (author) {
            authorEl.innerHTML = highlightString(author);
        } else {
            authorEl.innerText = "";
        }
        
        // 🌟 時間差アニメーション（メインの言葉が出てから、800ms後にタイトルを表示）
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                quoteEl.classList.add('fade-in');
                setTimeout(() => {
                    sourceEl.classList.add('fade-in');
                }, 800);
            });
        });
    };
    
    // 🌉 iOS / Android 両対応の通信ブリッジ
    function callNative(handlerName, body = null) {
        // 【1】iOS (WebKit) の場合の通信ルート
        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers[handlerName]) {
            window.webkit.messageHandlers[handlerName].postMessage(body || "");
        }
        // 【2】Android の場合の通信ルート
        else if (window.AndroidBridge && typeof window.AndroidBridge[handlerName] === 'function') {
            const payload = (typeof body === 'object') ? JSON.stringify(body) : (body || "");
            window.AndroidBridge[handlerName](payload);
        }
        // 【3】ブラウザ等で開かれた場合のエラー回避
        else {
            console.warn(`Native handler [${handlerName}] is not available on this platform.`);
        }
    }
    
    function searchCurrentBook() {
        const title = currentQuoteData.title.replace(/^- |-$/g, '').trim();
        const author = currentQuoteData.author.trim();
        if (title || author) {
            callNative('searchBook', { title, author });
        }
    }
    
    function setupEventListeners() {
        document.getElementById('source-area')?.addEventListener('click', searchCurrentBook);
        
        document.getElementById('btn-settings')?.addEventListener('click', () => {
            callNative('triggerHaptic');
            callNative('showSettings');
        });
        
        document.getElementById('btn-bag')?.addEventListener('click', () => {
            callNative('triggerHaptic');
            callNative('showFavorites');
        });
        
        document.getElementById('btn-help')?.addEventListener('click', () => {
            if (currentQuoteData.text) callNative('explainQuote', currentQuoteData.text);
        });
        
        document.getElementById('btn-book')?.addEventListener('click', () => {
            if (currentQuoteData.text) {
                callNative('triggerHaptic');
                
                let textToSpeak = currentQuoteData.text;
                if (currentQuoteData.title) {
                    textToSpeak += "|||" + currentQuoteData.title;
                }
                if (currentQuoteData.author) {
                    textToSpeak += "|||by " + currentQuoteData.author;
                }
                
                callNative('speakText', textToSpeak);
            }
        });
        
        document.getElementById('btn-star')?.addEventListener('click', (e) => {
            if (currentQuoteData.text) {
                callNative('triggerHaptic');
                
                const icon = e.currentTarget.querySelector('svg');
                if (icon) {
                    if (icon.classList.contains('stocked')) {
                        callNative('unstockQuote', currentQuoteData.text);
                        icon.style.transition = 'transform 0.2s, fill 0.2s, color 0.2s';
                        icon.style.transform = "scale(0.8)";
                        icon.style.fill = "none";
                        icon.style.color = "";
                        icon.classList.remove('stocked');
                        setTimeout(() => { icon.style.transform = "scale(1)"; }, 200);
                    } else {
                        callNative('stockQuote', currentQuoteData.text);
                        icon.style.transition = 'transform 0.2s, fill 0.2s, color 0.2s';
                        icon.style.transform = "scale(1.3)";
                        icon.style.fill = "#ff9500";
                        icon.style.color = "#ff9500";
                        icon.classList.add('stocked');
                        setTimeout(() => { icon.style.transform = "scale(1)"; }, 300);
                    }
                }
            }
        });
        
        const quoteElement = document.getElementById('quote-text');
        let pressTimer;
        
        quoteElement?.addEventListener('touchstart', (e) => {
            if (e.target.classList.contains('word') || e.target.classList.contains('highlight-word')) {
                const word = e.target.innerText.replace(/[^a-zA-Z'-]/g, '');
                if (word.length > 0) {
                    pressTimer = setTimeout(() => {
                        callNative('triggerHaptic');
                        callNative('showNativeTranslation', word);
                    }, 400);
                }
            }
        }, { passive: true });
        
        quoteElement?.addEventListener('touchend', () => clearTimeout(pressTimer));
        quoteElement?.addEventListener('touchmove', () => clearTimeout(pressTimer));
        
        document.addEventListener('selectionchange', () => {
            const selectedText = window.getSelection()?.toString().trim();
            if (selectedText && selectedText.length > 0 && selectedText.length < 50) {
                callNative('showNativeTranslation', selectedText);
            }
        });
        
        let startX = 0, startY = 0;
        document.addEventListener('touchstart', (e) => {
            startX = e.changedTouches[0].screenX;
            startY = e.changedTouches[0].screenY;
        }, { passive: true });
        
        document.addEventListener('touchend', (e) => {
            const diffX = e.changedTouches[0].screenX - startX;
            const diffY = e.changedTouches[0].screenY - startY;
            
							if (Math.abs(diffX) > 50 && Math.abs(diffY) < 100) {
							window.getSelection()?.removeAllRanges();
							// 左右どちらも新しいランダム（履歴めくりはしない）
							callNative('requestNextQuote');
							}
        }, { passive: true });
    }
    
    window.addEventListener('load', () => {
        setupEventListeners();
    });
    
})();
