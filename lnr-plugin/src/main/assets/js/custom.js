/**
 * LNR Legado Plugin - Paragraph Review (段评) Script
 * Based on Legado lyc486's ReviewRule implementation
 *
 * This script is injected into the WebView to enable paragraph-level comments.
 * It uses the ReviewRule from the book source to fetch and display reviews.
 *
 * Flow:
 * 1. Parse all <p> elements as paragraphs
 * 2. Add a clickable SVG icon at the end of each paragraph
 * 3. On click, fetch reviews from reviewUrl (with paragraph index)
 * 4. Display reviews in a floating popover panel
 */
(function() {
    'use strict';

    // ==================== Configuration ====================
    var CONFIG = {
        ICON_SIZE: 14,
        ICON_COLOR: '#4CAF50',
        ICON_HOVER: '#FF9800',
        POPOVER_MAX_WIDTH: 320,
        POPOVER_MAX_HEIGHT: 400,
        DEBOUNCE_MS: 200,
        REVIEW_API: '/api/v1/reviews'
    };

    // ==================== State ====================
    var state = {
        activePopover: null,
        activeParagraph: null,
        reviewCache: {},
        sourceConfig: null
    };

    // ==================== SVG Icon Generator ====================
    function createCommentIcon(count) {
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('width', CONFIG.ICON_SIZE);
        svg.setAttribute('height', CONFIG.ICON_SIZE);
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.style.cssText = 'vertical-align:middle;margin-left:4px;cursor:pointer;opacity:0.6;transition:opacity 0.2s;';
        svg.innerHTML = '<path fill="' + CONFIG.ICON_COLOR + '" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>' +
            (count > 0 ? '<text x="12" y="16" text-anchor="middle" fill="' + CONFIG.ICON_COLOR + '" font-size="10">' + (count > 99 ? '99+' : count) + '</text>' : '');
        svg.addEventListener('mouseenter', function() { svg.style.opacity = '1'; svg.querySelector('path').setAttribute('fill', CONFIG.ICON_HOVER); });
        svg.addEventListener('mouseleave', function() { svg.style.opacity = '0.6'; svg.querySelector('path').setAttribute('fill', CONFIG.ICON_COLOR); });
        return svg;
    }

    // ==================== Review Data Fetcher ====================
    function fetchReviews(paragraphIndex, callback) {
        if (state.reviewCache[paragraphIndex]) {
            callback(state.reviewCache[paragraphIndex]);
            return;
        }
        // Request reviews from the native bridge
        if (window.LegadoReviewBridge) {
            window.LegadoReviewBridge.getReviews(paragraphIndex, function(jsonStr) {
                try {
                    var reviews = JSON.parse(jsonStr);
                    state.reviewCache[paragraphIndex] = reviews;
                    callback(reviews);
                } catch(e) { callback([]); }
            });
        } else {
            // Fallback: use XMLHttpRequest to the review API
            var xhr = new XMLHttpRequest();
            var url = CONFIG.REVIEW_API + '?paragraph=' + encodeURIComponent(paragraphIndex) +
                      '&chapter=' + encodeURIComponent(getCurrentChapterId());
            xhr.open('GET', url, true);
            xhr.onload = function() {
                if (xhr.status === 200) {
                    try {
                        var reviews = JSON.parse(xhr.responseText);
                        state.reviewCache[paragraphIndex] = reviews;
                        callback(reviews);
                    } catch(e) { callback([]); }
                } else { callback([]); }
            };
            xhr.onerror = function() { callback([]); };
            xhr.send();
        }
    }

    function getCurrentChapterId() {
        var meta = document.querySelector('meta[name="chapter-id"]');
        return meta ? meta.getAttribute('content') : '';
    }

    // ==================== Popover Panel ====================
    function createPopover(paragraphIndex, anchorElement) {
        closePopover();

        var popover = document.createElement('div');
        popover.id = 'legado-review-popover';
        popover.style.cssText = 'position:fixed;z-index:99999;background:#fff;border:1px solid #ddd;border-radius:8px;' +
            'box-shadow:0 4px 16px rgba(0,0,0,0.15);max-width:' + CONFIG.POPOVER_MAX_WIDTH + 'px;' +
            'max-height:' + CONFIG.POPOVER_MAX_HEIGHT + 'px;overflow-y:auto;padding:12px;font-size:14px;color:#333;';

        // Position near the anchor
        var rect = anchorElement.getBoundingClientRect();
        var top = Math.min(rect.bottom + 4, window.innerHeight - CONFIG.POPOVER_MAX_HEIGHT - 10);
        var left = Math.min(rect.left, window.innerWidth - CONFIG.POPOVER_MAX_WIDTH - 10);
        popover.style.top = top + 'px';
        popover.style.left = left + 'px';

        // Loading indicator
        popover.innerHTML = '<div style="text-align:center;color:#999;padding:16px;">加载评论中...</div>';
        document.body.appendChild(popover);
        state.activePopover = popover;
        state.activeParagraph = paragraphIndex;

        // Fetch and render reviews
        fetchReviews(paragraphIndex, function(reviews) {
            if (!state.activePopover || state.activePopover !== popover) return;
            if (!reviews || reviews.length === 0) {
                popover.innerHTML = '<div style="text-align:center;color:#999;padding:16px;">暂无评论</div>';
                return;
            }
            var html = '<div style="font-weight:bold;margin-bottom:8px;border-bottom:1px solid #eee;padding-bottom:6px;">段评 (' + reviews.length + ')</div>';
            reviews.forEach(function(r) {
                html += '<div style="margin-bottom:10px;padding-bottom:8px;border-bottom:1px solid #f5f5f5;">';
                if (r.avatar) {
                    html += '<img src="' + escapeHtml(r.avatar) + '" style="width:24px;height:24px;border-radius:50%;vertical-align:middle;margin-right:6px;"/>';
                }
                if (r.content) {
                    html += '<span style="line-height:1.5;">' + escapeHtml(r.content) + '</span>';
                }
                if (r.time) {
                    html += '<div style="font-size:11px;color:#999;margin-top:2px;">' + escapeHtml(r.time) + '</div>';
                }
                html += '</div>';
            });
            popover.innerHTML = html;
        });
    }

    function closePopover() {
        if (state.activePopover) {
            state.activePopover.remove();
            state.activePopover = null;
            state.activeParagraph = null;
        }
    }

    // ==================== Paragraph Processing ====================
    function processParagraphs() {
        var paragraphs = document.querySelectorAll('.chapter-content p, #chapter-content p, [data-paragraph]');
        if (paragraphs.length === 0) {
            paragraphs = document.querySelectorAll('p');
        }

        paragraphs.forEach(function(p, index) {
            if (p.dataset.reviewProcessed) return;
            p.dataset.reviewProcessed = 'true';
            p.dataset.paragraphIndex = index;

            var icon = createCommentIcon(0);
            icon.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                if (state.activeParagraph === index) {
                    closePopover();
                } else {
                    createPopover(index, icon);
                }
            });
            p.appendChild(icon);
        });
    }

    // ==================== Utilities ====================
    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // ==================== External API ====================
    window.LegadoReview = {
        config: function(cfg) {
            if (cfg) {
                Object.keys(cfg).forEach(function(k) { CONFIG[k] = cfg[k]; });
            }
        },
        refresh: function() {
            document.querySelectorAll('[data-review-processed]').forEach(function(el) {
                delete el.dataset.reviewProcessed;
            });
            state.reviewCache = {};
            processParagraphs();
        },
        close: closePopover,
        setCache: function(index, data) {
            state.reviewCache[index] = data;
        }
    };

    // ==================== Initialize ====================
    function init() {
        processParagraphs();
        // Close popover on outside click
        document.addEventListener('click', function(e) {
            if (state.activePopover && !state.activePopover.contains(e.target) &&
                !e.target.closest('svg')) {
                closePopover();
            }
        });
        // Re-process on dynamic content load
        var observer = new MutationObserver(function(mutations) {
            var needProcess = mutations.some(function(m) { return m.addedNodes.length > 0; });
            if (needProcess) setTimeout(processParagraphs, 100);
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
