/**
 * LNR Legado Plugin - custom.js
 * Injected into WebView for paragraph-level comment functionality.
 *
 * Features:
 *   - SVG comment icon generation at paragraph end
 *   - Click event binding for comment popover
 *   - Coordinate-based positioning for comment overlays
 *   - Comment data fetch and display in floating panel
 */

(function () {
    'use strict';

    // ==================== Configuration ====================
    const CONFIG = {
        COMMENT_ICON_SIZE: 16,
        COMMENT_ICON_COLOR: '#4CAF50',
        COMMENT_ICON_HOVER: '#FF9800',
        COMMENT_POPOVER_MAX_WIDTH: 320,
        COMMENT_POPOVER_MAX_HEIGHT: 400,
        COMMENT_API_BASE: '/api/v1/comments',
        DEBOUNCE_MS: 300
    };

    // ==================== SVG Icon Generator ====================
    /**
     * Create SVG comment bubble icon.
     * @param {number} count - Comment count (0 for empty)
     * @returns {string} SVG markup
     */
    function createCommentIconSVG(count) {
        const size = CONFIG.COMMENT_ICON_SIZE;
        const color = count > 0 ? CONFIG.COMMENT_ICON_COLOR : '#9E9E9E';
        const displayCount = count > 99 ? '99+' : count.toString();

        return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" class="lnr-comment-icon" data-count="${count}">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"
                  fill="${color}" fill-opacity="0.15" stroke="${color}" stroke-width="1.5"/>
            ${count > 0 ? `<text x="12" y="14" text-anchor="middle" font-size="9" font-weight="bold" fill="${color}">${displayCount}</text>` : ''}
        </svg>`;
    }

    // ==================== Comment Icon Injection ====================
    /**
     * Inject comment icons at the end of each paragraph.
     */
    function injectCommentIcons() {
        const paragraphs = document.querySelectorAll('.content-line, .chapter-content p, .read-content p, p');

        paragraphs.forEach((p, index) => {
            // Skip if already has comment icon
            if (p.querySelector('.lnr-comment-trigger')) return;
            // Skip empty paragraphs
            if (!p.textContent.trim()) return;

            const trigger = document.createElement('span');
            trigger.className = 'lnr-comment-trigger';
            trigger.dataset.paragraphIndex = index;
            trigger.innerHTML = createCommentIconSVG(0);
            trigger.style.cssText = `
                display: inline-block;
                cursor: pointer;
                vertical-align: middle;
                margin-left: 4px;
                opacity: 0.6;
                transition: opacity 0.2s ease, transform 0.2s ease;
            `;

            // Hover effects
            trigger.addEventListener('mouseenter', () => {
                trigger.style.opacity = '1';
                trigger.style.transform = 'scale(1.2)';
            });
            trigger.addEventListener('mouseleave', () => {
                trigger.style.opacity = '0.6';
                trigger.style.transform = 'scale(1)';
            });

            // Click handler
            trigger.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                showCommentPopover(trigger, index);
            });

            p.appendChild(trigger);
        });
    }

    // ==================== Comment Popover ====================
    let activePopover = null;

    /**
     * Show comment popover positioned relative to the trigger element.
     * @param {HTMLElement} trigger - The clicked comment icon
     * @param {number} paragraphIndex - Index of the paragraph
     */
    function showCommentPopover(trigger, paragraphIndex) {
        // Close existing popover
        closePopover();

        const rect = trigger.getBoundingClientRect();
        const popover = document.createElement('div');
        popover.className = 'lnr-comment-popover';
        popover.innerHTML = `
            <div class="lnr-popover-header">
                <span class="lnr-popover-title">段落评论</span>
                <button class="lnr-popover-close">&times;</button>
            </div>
            <div class="lnr-popover-content">
                <div class="lnr-comment-loading">加载评论中...</div>
            </div>
            <div class="lnr-popover-input">
                <input type="text" placeholder="写下你的评论..." class="lnr-comment-input" />
                <button class="lnr-comment-submit">发送</button>
            </div>
        `;

        // Position the popover
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        let left = rect.left;
        let top = rect.bottom + 8;

        // Ensure popover doesn't overflow viewport
        if (left + CONFIG.COMMENT_POPOVER_MAX_WIDTH > viewportWidth) {
            left = viewportWidth - CONFIG.COMMENT_POPOVER_MAX_WIDTH - 16;
        }
        if (top + CONFIG.COMMENT_POPOVER_MAX_HEIGHT > viewportHeight) {
            top = rect.top - CONFIG.COMMENT_POPOVER_MAX_HEIGHT - 8;
        }

        popover.style.cssText = `
            position: fixed;
            left: ${Math.max(8, left)}px;
            top: ${Math.max(8, top)}px;
            width: ${CONFIG.COMMENT_POPOVER_MAX_WIDTH}px;
            max-height: ${CONFIG.COMMENT_POPOVER_MAX_HEIGHT}px;
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.15);
            z-index: 10000;
            display: flex;
            flex-direction: column;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            font-size: 14px;
            overflow: hidden;
            animation: lnr-popover-in 0.2s ease-out;
        `;

        document.body.appendChild(popover);
        activePopover = popover;

        // Add animation keyframes if not exists
        if (!document.getElementById('lnr-popover-styles')) {
            const style = document.createElement('style');
            style.id = 'lnr-popover-styles';
            style.textContent = `
                @keyframes lnr-popover-in {
                    from { opacity: 0; transform: translateY(-8px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .lnr-popover-header {
                    display: flex; justify-content: space-between; align-items: center;
                    padding: 12px 16px; border-bottom: 1px solid #eee;
                }
                .lnr-popover-title { font-weight: 600; color: #333; }
                .lnr-popover-close {
                    background: none; border: none; font-size: 20px;
                    cursor: pointer; color: #999; padding: 0 4px;
                }
                .lnr-popover-close:hover { color: #333; }
                .lnr-popover-content {
                    flex: 1; overflow-y: auto; padding: 12px 16px;
                    max-height: 280px;
                }
                .lnr-comment-loading { text-align: center; color: #999; padding: 20px; }
                .lnr-comment-item {
                    padding: 8px 0; border-bottom: 1px solid #f5f5f5;
                }
                .lnr-comment-item:last-child { border-bottom: none; }
                .lnr-comment-author { font-weight: 500; color: #1976D2; font-size: 13px; }
                .lnr-comment-text { margin-top: 4px; color: #333; line-height: 1.5; }
                .lnr-comment-time { font-size: 11px; color: #bbb; margin-top: 4px; }
                .lnr-popover-input {
                    display: flex; padding: 8px 12px; border-top: 1px solid #eee;
                    gap: 8px;
                }
                .lnr-comment-input {
                    flex: 1; border: 1px solid #ddd; border-radius: 20px;
                    padding: 8px 14px; font-size: 13px; outline: none;
                }
                .lnr-comment-input:focus { border-color: #4CAF50; }
                .lnr-comment-submit {
                    background: #4CAF50; color: #fff; border: none;
                    border-radius: 20px; padding: 8px 16px; cursor: pointer;
                    font-size: 13px; font-weight: 500;
                }
                .lnr-comment-submit:hover { background: #43A047; }
                .lnr-no-comments { text-align: center; color: #bbb; padding: 20px; }
            `;
            document.head.appendChild(style);
        }

        // Bind events
        popover.querySelector('.lnr-popover-close').addEventListener('click', closePopover);
        popover.querySelector('.lnr-comment-submit').addEventListener('click', () => {
            const input = popover.querySelector('.lnr-comment-input');
            const text = input.value.trim();
            if (text) {
                submitComment(paragraphIndex, text);
                input.value = '';
            }
        });

        // Fetch comments
        fetchComments(paragraphIndex, popover);

        // Close on outside click
        setTimeout(() => {
            document.addEventListener('click', outsideClickHandler);
        }, 100);
    }

    function closePopover() {
        if (activePopover) {
            activePopover.remove();
            activePopover = null;
            document.removeEventListener('click', outsideClickHandler);
        }
    }

    function outsideClickHandler(e) {
        if (activePopover && !activePopover.contains(e.target) &&
            !e.target.closest('.lnr-comment-trigger')) {
            closePopover();
        }
    }

    // ==================== Comment Data ====================
    /**
     * Fetch comments for a paragraph from LNR API.
     */
    async function fetchComments(paragraphIndex, popover) {
        const content = popover.querySelector('.lnr-popover-content');
        try {
            const chapterUrl = window.location.href;
            const response = await fetch(
                `${CONFIG.COMMENT_API_BASE}?chapter=${encodeURIComponent(chapterUrl)}&paragraph=${paragraphIndex}`
            );
            if (!response.ok) throw new Error('API error');
            const comments = await response.json();

            if (!comments || comments.length === 0) {
                content.innerHTML = '<div class="lnr-no-comments">暂无评论，来抢沙发吧~</div>';
                return;
            }

            content.innerHTML = comments.map(c => `
                <div class="lnr-comment-item">
                    <div class="lnr-comment-author">${escapeHtml(c.userName || '匿名')}</div>
                    <div class="lnr-comment-text">${escapeHtml(c.content)}</div>
                    <div class="lnr-comment-time">${formatTime(c.timestamp)}</div>
                </div>
            `).join('');
        } catch (e) {
            content.innerHTML = '<div class="lnr-no-comments">暂无评论，来抢沙发吧~</div>';
        }
    }

    /**
     * Submit a comment for a paragraph.
     */
    async function submitComment(paragraphIndex, text) {
        try {
            const chapterUrl = window.location.href;
            await fetch(CONFIG.COMMENT_API_BASE, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    chapter: chapterUrl,
                    paragraph: paragraphIndex,
                    content: text
                })
            });
            // Refresh comments
            if (activePopover) {
                fetchComments(paragraphIndex, activePopover);
            }
        } catch (e) {
            console.error('Failed to submit comment:', e);
        }
    }

    // ==================== Utilities ====================
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatTime(timestamp) {
        if (!timestamp) return '';
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        if (diff < 60000) return '刚刚';
        if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
        if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
        return `${date.getMonth() + 1}月${date.getDate()}日`;
    }

    // ==================== Initialization ====================
    /**
     * Initialize the comment system.
     * Called by LNR after content is loaded in WebView.
     */
    function init() {
        // Inject icons when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                setTimeout(injectCommentIcons, 500);
            });
        } else {
            setTimeout(injectCommentIcons, 500);
        }

        // Re-inject on content changes (for paginated content)
        const observer = new MutationObserver((mutations) => {
            let shouldReinject = false;
            mutations.forEach(m => {
                if (m.addedNodes.length > 0) shouldReinject = true;
            });
            if (shouldReinject) {
                setTimeout(injectCommentIcons, 200);
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    // Expose API for LNR host
    window.LNRComments = {
        init: init,
        injectIcons: injectCommentIcons,
        closePopover: closePopover
    };

    // Auto-init
    init();
})();
