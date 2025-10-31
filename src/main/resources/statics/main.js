// ===== COSTANTI E CONFIGURAZIONE =====
const CONFIG = {
    SPINNER_DELAY: 1000,
    LOCAL_HOST: 'http://127.0.0.1:8090',
    SELECTORS: {
        SPINNER: 'loadingSpinner',
        PACKAGES_FILTER: 'packagesFilter',
        EXPRESSION_INPUT: 'expressionInput',
        ENABLE_TRACE: 'enableTrace',
        TAB_BUTTONS: '.tab-button',
        TAB_CONTENTS: '.tab-content',
        LIST_TRACES: 'listTraces',
        ERROR_MSG: 'errorMsg',
        STACK_TREE: 'stackTreeContainer',
        TOP_METHODS: 'topMethodsContainer',
        SEARCH_BAR: 'search-bar'
    },
    API_ENDPOINTS: {
        TRACE_START: '/api/trace/start',
        TRACE_STOP: '/api/trace/stop',
        TRACE_DELETE: '/api/trace/delete/',
        TRACE_SETUP: '/api/trace/setup',
        TRACES_LIST: '/api/traces',
        TRACE_GET: '/api/trace/'
    }
};

// ===== UTILITÀ GENERALI =====
const Utils = {
    getContextRoot() {
        return (document.location.protocol === 'file:' || document.location.href.indexOf('/javaperformanceagent/performance-agent/') > -1) ? CONFIG.LOCAL_HOST : '';
    },

    getElementById(id) {
        return document.getElementById(id);
    },

    createElement(tag, className = '', textContent = '') {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (textContent) element.textContent = textContent;
        return element;
    }
};

// ===== GESTIONE SPINNER =====
const SpinnerManager = {
    show() {
        Utils.getElementById(CONFIG.SELECTORS.SPINNER).style.display = 'block';
    },

    hide() {
        setTimeout(() => {
            Utils.getElementById(CONFIG.SELECTORS.SPINNER).style.display = 'none';
        }, CONFIG.SPINNER_DELAY);
    }
};

// ===== GESTORE API =====
const ApiManager = {
    async makeRequest(url, options = {}) {
        SpinnerManager.show();
        try {
            const response = await fetch(Utils.getContextRoot() + url, options);
            if (!response.ok) throw new Error('Errore nella chiamata API');
            return response;
        } finally {
            SpinnerManager.hide();
        }
    },

    async startTrace(expression, packagesFilter) {
        const url = `${CONFIG.API_ENDPOINTS.TRACE_START}?expression=${encodeURIComponent(expression)}&packagesFilter=${encodeURIComponent(packagesFilter)}`;
        await this.makeRequest(url, {method: 'POST'});
    },

    async stopTrace() {
        await this.makeRequest(CONFIG.API_ENDPOINTS.TRACE_STOP, {method: 'POST'});
    },

    async deleteTrace(traceId) {
        const url = `${CONFIG.API_ENDPOINTS.TRACE_DELETE}${encodeURIComponent(traceId)}`;
        await this.makeRequest(url, {method: 'POST'});
    },

    async getTraceSetup() {
        const response = await this.makeRequest(CONFIG.API_ENDPOINTS.TRACE_SETUP);
        return response.json();
    },

    async getTraces() {
        const response = await this.makeRequest(CONFIG.API_ENDPOINTS.TRACES_LIST);
        return response.json();
    },

    async getTrace(fileName) {
        const url = `${CONFIG.API_ENDPOINTS.TRACE_GET}${encodeURIComponent(fileName)}`;
        const response = await this.makeRequest(url);
        return response.text();
    }
};

// ===== GESTIONE ERRORI =====
const ErrorHandler = {
    showError(message) {
        const errorElement = Utils.getElementById(CONFIG.SELECTORS.ERROR_MSG);
        errorElement.textContent = message;
        console.error(message);
    },

    clearError() {
        Utils.getElementById(CONFIG.SELECTORS.ERROR_MSG).textContent = '';
    },

    handleApiError(error, fallbackMessage) {
        console.error(error);
        this.showError(fallbackMessage);
    }
};

// ===== GESTIONE TAB =====
const TabManager = {
    activate(index) {
        const buttons = document.querySelectorAll(CONFIG.SELECTORS.TAB_BUTTONS);
        const contents = document.querySelectorAll(CONFIG.SELECTORS.TAB_CONTENTS);

        buttons.forEach((btn, i) => {
            const isActive = (i === index);
            btn.classList.toggle("active", isActive);
            btn.setAttribute("aria-selected", isActive);
            contents[i].classList.toggle("active", isActive);
        });
    },

    initialize() {
        document.querySelectorAll(CONFIG.SELECTORS.TAB_BUTTONS).forEach((btn, i) => {
            btn.addEventListener("click", () => this.activate(i));
        });
    }
};

// ===== PROCESSORE DATI TRACE =====
const TraceProcessor = {
    parseToJSON(lines) {
        const stack = [];
        const root = {calls: []};
        stack.push({node: root, level: -1});

        const parseLine = (line) => {
            const match = line.match(/\+ (.*)\|(.*)>(.*)$/);
            if (!match) return null;
            const spaces = line.split('+ ')[0].length;
            return {
                level: spaces / 2,
                method: match[2].trim(),
                time: match[3]
            };
        };

        for (const line of lines) {
            const parsed = parseLine(line);
            if (!parsed) continue;

            const callObj = {
                method: parsed.method,
                executionTime: parsed.time
            };

            while (stack.length > 0 && stack[stack.length - 1].level >= parsed.level) {
                stack.pop();
            }

            const parent = stack[stack.length - 1].node;
            if (!parent.calls) parent.calls = [];
            parent.calls.push(callObj);
            stack.push({node: callObj, level: parsed.level});
        }

        return root;
    },

    getCallAndEndOfMethods(traceInput) {

        const timingForLines = {};
        const startMethodLines = [];
        const traceLines = traceInput.split('\n');

        // Elabora le linee
        for (const line of traceLines) {
            if (!line) continue;

            if (line.includes('- ')) {
                const [prefix, timing] = line.split('>');
                timingForLines[prefix.replace('-', '+')] = timing;
            } else {
                startMethodLines.push(line);
            }
        }
        return {timingForLines, startMethodLines};
    },

    parseInputToTree(timingForLines, startMethodLines) {

        // Crea output con timing
        const outputLines = startMethodLines
            .filter(line => (Number(timingForLines[line]) / 1000000000) > 0.001)
            .map(line =>
                `${line}>${Number(timingForLines[line]) / 1000000000}`
            );

        return this.parseToJSON(outputLines);
    },

    parseInputToTopMethodCall(timingForLines, startMethodLines) {

        const methodsByCall = {};

        for (const line of startMethodLines) {
            const timeOfExecution = Number(timingForLines[line]) / 1000000000;
            const methodKey = line.split('|')[1];

            if (!(methodKey in methodsByCall)) {
                methodsByCall[methodKey] = {
                    methodKey,
                    totalCalls: 0,
                    totalTime: 0
                };
            }

            const methodStats = methodsByCall[methodKey];
            methodStats.totalCalls++;
            methodStats.totalTime += timeOfExecution;
        }

        return Object.values(methodsByCall);
    }
};

// ===== RENDERER UI =====
const UIRenderer = {
    selectedListItem: null,
    lastHighlightChild: null,

    renderFileList(files) {
        const ul = Utils.getElementById(CONFIG.SELECTORS.LIST_TRACES);
        ul.innerHTML = '';

        if (!files || files.length === 0) {
            ul.innerHTML = '<li>No Traces Available!</li>';
            return;
        }

        files.forEach(file => {
            if (file.indexOf(".") !== 0) {
                ul.appendChild(this.createFileListItem(file));
            }
        });
    },

    createFileListItem(file) {
        const li = Utils.createElement('li');

        const deleteBtn = Utils.createElement('button', 'delete-trace', 'Delete Trace');
        deleteBtn.onclick = (e) => {
            e.preventDefault();
            e.stopImmediatePropagation();
            TraceActions.deleteTrace(file.split('|')[0]);
        };

        const title = Utils.createElement('span', '', file);
        title.tabIndex = 0;
        li.onclick = () => this.selectTrace(file, li);

        li.appendChild(deleteBtn);
        li.appendChild(title);
        return li;
    },

    async selectTrace(file, listItem) {
        if (this.selectedListItem) {
            this.selectedListItem.classList.remove('selected');
        }
        listItem.classList.add('selected');
        this.selectedListItem = listItem;
        TabManager.activate(2);
        await TraceActions.renderTrace(file);
    },

    createStackedViewHeader(node, parent) {
        const header = Utils.createElement('div', 'tree-header');

        if (node.calls && node.calls.length) {
            const button = Utils.createElement('span', 'red-circle');
            button.title = "Set As Root Node";
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                this.renderStackTree(node.calls);
            });
            header.appendChild(button);
        }

        const methodSpan = Utils.createElement('span', 'method', node.method || '(no method)');
        if (!node.calls) methodSpan.classList.add('method-no-childred');

        const timePercentage = parent ?
            (Number(node.executionTime) / Number(parent.executionTime) * 100) : 100;

        header.appendChild(methodSpan);
        header.appendChild(this.createTimeContainer(node.executionTime, timePercentage));
        return header;
    },

    createTimeContainer(executionTime, percentage) {
        const container = Utils.createElement('div', 'text-container');
        const background = Utils.createElement('span', 'background');
        background.style.width = `${percentage}%`;
        const textSpan = Utils.createElement('span', 'text', `${executionTime} s`);

        container.appendChild(background);
        container.appendChild(textSpan);
        return container;
    },

    createTreeNode(node, parent = null) {
        const container = Utils.createElement('div', 'tree-node');
        const header = this.createStackedViewHeader(node, parent);
        container.appendChild(header);

        let childrenContainer = null;
        header.addEventListener('click', () => {
            if (!childrenContainer && node.calls && node.calls.length) {
                childrenContainer = Utils.createElement('div', 'children');
                node.calls.forEach(child => {
                    childrenContainer.appendChild(this.createTreeNode(child, node));
                });
                container.appendChild(childrenContainer);

                if (this.lastHighlightChild) {
                    this.lastHighlightChild.classList.remove('highlight-last');
                }
                childrenContainer.classList.add('highlight-last');
                this.lastHighlightChild = childrenContainer;
                container.classList.add('expanded');
            } else if (childrenContainer) {
                container.removeChild(childrenContainer);
                childrenContainer = null;
            }
        });

        return container;
    },

    renderStackTree(calls) {
        const container = Utils.getElementById(CONFIG.SELECTORS.STACK_TREE);
        container.innerHTML = '';
        ErrorHandler.clearError();

        calls.forEach(rootNode => {
            container.appendChild(this.createTreeNode(rootNode));
        });
    },

    renderStackedView(timingForLines, startMethodLines) {
        const data = TraceProcessor.parseInputToTree(timingForLines, startMethodLines);
        this.renderStackTree(data.calls);
    },

    createTopMethodHeader(node, totalTime) {
        const timePercentage = (Number(node.totalTime) / Number(totalTime)) * 100;
        const domNode = Utils.createElement('div', 'tree-node');
        const header = Utils.createElement('div', 'tree-header');

        const methodSpan = Utils.createElement('span', 'method', node.methodKey || '(no method)');
        const callContainer = Utils.createElement('div', 'text-container');
        const callSpan = Utils.createElement('span', 'text', `${node.totalCalls} calls`);
        const timeContainer = this.createTimeContainer(node.totalTime, timePercentage);

        callContainer.appendChild(callSpan);
        header.appendChild(methodSpan);
        header.appendChild(callContainer);
        header.appendChild(timeContainer);
        domNode.appendChild(header);

        return domNode;
    },

    renderTopMethodByCall(timingForLines, startMethodLines) {
        const data = TraceProcessor.parseInputToTopMethodCall(timingForLines, startMethodLines);
        const sortedMethods = data.sort((a, b) => b.totalTime - a.totalTime);
        const container = Utils.getElementById(CONFIG.SELECTORS.TOP_METHODS);

        container.innerHTML = '';
        ErrorHandler.clearError();

        const maxTime = sortedMethods[0]?.totalTime || 1;
        sortedMethods.forEach(method => {
            container.appendChild(this.createTopMethodHeader(method, maxTime));
        });
    }
};

// ===== AZIONI TRACE =====
const TraceActions = {
    async startTrace() {
        try {
            const expression = Utils.getElementById(CONFIG.SELECTORS.EXPRESSION_INPUT).value;
            const packagesFilter = Utils.getElementById(CONFIG.SELECTORS.PACKAGES_FILTER).value;
            await ApiManager.startTrace(expression, packagesFilter);
            await this.showTraceSetup();
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Errore avvio trace');
        }
    },

    async stopTrace() {
        try {
            await ApiManager.stopTrace();
            await this.showTraceSetup();
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Errore stop trace');
        }
    },

    async deleteTrace(traceId) {
        try {
            await ApiManager.deleteTrace(traceId);
            await this.fetchTraces();
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Errore eliminazione trace');
        }
    },

    async showTraceSetup() {
        try {
            const setup = await ApiManager.getTraceSetup();
            Utils.getElementById(CONFIG.SELECTORS.ENABLE_TRACE).checked = setup.enabled;
            Utils.getElementById(CONFIG.SELECTORS.PACKAGES_FILTER).value = setup.packagesFilter;
            Utils.getElementById(CONFIG.SELECTORS.EXPRESSION_INPUT).value = setup.expression;
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Errore caricamento setup');
        }
    },

    async fetchTraces() {
        try {
            const files = await ApiManager.getTraces();
            UIRenderer.renderFileList(files);
            ErrorHandler.clearError();
        } catch (error) {
            const ul = Utils.getElementById(CONFIG.SELECTORS.LIST_TRACES);
            ul.innerHTML = '<li>Impossibile caricare la lista file.</li>';
            ErrorHandler.handleApiError(error, 'Errore caricamento lista tracce');
        }
    },

    async renderTrace(fileName) {
        try {
            const text = await ApiManager.getTrace(fileName);
            this.convertAndRender(text);
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Impossibile scaricare il file JSON');
            alert('Impossibile scaricare il file JSON.');
        }
    },

    convertAndRender(trace) {
        let {timingForLines, startMethodLines} = TraceProcessor.getCallAndEndOfMethods(trace);
        UIRenderer.renderStackedView(timingForLines, startMethodLines);
        UIRenderer.renderTopMethodByCall(timingForLines, startMethodLines);
    }
};

// ===== FILTRO RICERCA =====
const SearchManager = {
    filterTraces() {
        const input = Utils.getElementById(CONFIG.SELECTORS.SEARCH_BAR);
        const filter = input.value.toUpperCase();
        const ul = Utils.getElementById(CONFIG.SELECTORS.LIST_TRACES);
        const items = ul.getElementsByTagName("li");

        Array.from(items).forEach(item => {
            const txtValue = item.textContent || item.innerText;
            const isVisible = txtValue.toUpperCase().indexOf(filter) > -1;
            item.style.display = isVisible ? "" : "none";
        });
    }
};

// ===== INIZIALIZZAZIONE =====
const AppInitializer = {
    async init() {
        try {
            await TraceActions.fetchTraces();
            await TraceActions.showTraceSetup();
            TabManager.initialize();
        } catch (error) {
            ErrorHandler.handleApiError(error, 'Errore inizializzazione applicazione');
        }
    }
};

// ===== ESPOSIZIONE FUNZIONI GLOBALI =====
window.startTrace = () => TraceActions.startTrace();
window.stopTrace = () => TraceActions.stopTrace();
window.filterTraces = () => SearchManager.filterTraces();

// ===== AVVIO APPLICAZIONE =====
document.addEventListener('DOMContentLoaded', () => AppInitializer.init());
