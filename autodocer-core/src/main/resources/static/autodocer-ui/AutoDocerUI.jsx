import React, { useState, useEffect, useMemo } from 'react';
import { Search, Moon, Sun, Copy, Check, Loader, AlertTriangle, Code, CheckCircle, Inbox, ChevronRight, Menu, X } from 'lucide-react';

const AutoDocerUI = () => {
  const [apiSpec, setApiSpec] = useState(null);
  const [selectedServer, setSelectedServer] = useState(null);
  const [currentEndpoint, setCurrentEndpoint] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [darkMode, setDarkMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [generatingExamples, setGeneratingExamples] = useState(false);
  const [examples, setExamples] = useState([]);
  const [copiedIndex, setCopiedIndex] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // Load API spec
  useEffect(() => {
    loadApi();
  }, []);

  // Apply dark mode class to body
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [darkMode]);

  const loadApi = async () => {
    try {
      setLoading(true);
      const response = await fetch('/autodocer/api-docs');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      
      const data = await response.json();
      setApiSpec(data);
      
      // Set default server
      if (data.servers && data.servers.length > 0) {
        setSelectedServer(data.servers[0].url);
      } else {
        setSelectedServer('/');
      }
      
      setError(null);
    } catch (err) {
      console.error('Error loading API:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Group endpoints by tag
  const endpointsByTag = useMemo(() => {
    if (!apiSpec || !apiSpec.paths) return {};
    
    const grouped = {};
    Object.entries(apiSpec.paths).forEach(([path, methods]) => {
      Object.entries(methods).forEach(([method, operation]) => {
        const tag = (operation.tags && operation.tags[0]) || 'default';
        if (!grouped[tag]) grouped[tag] = [];
        grouped[tag].push({ path, method, operation });
      });
    });
    
    return grouped;
  }, [apiSpec]);

  // Filter endpoints based on search
  const filteredEndpoints = useMemo(() => {
    if (!searchQuery.trim()) return endpointsByTag;
    
    const query = searchQuery.toLowerCase();
    const filtered = {};
    
    Object.entries(endpointsByTag).forEach(([tag, endpoints]) => {
      const matchingEndpoints = endpoints.filter(({ path, method, operation }) => {
        return (
          path.toLowerCase().includes(query) ||
          method.toLowerCase().includes(query) ||
          (operation.summary && operation.summary.toLowerCase().includes(query)) ||
          (operation.description && operation.description.toLowerCase().includes(query))
        );
      });
      
      if (matchingEndpoints.length > 0) {
        filtered[tag] = matchingEndpoints;
      }
    });
    
    return filtered;
  }, [endpointsByTag, searchQuery]);

  const buildEndpointInfo = (path, method, operation) => {
    const parameters = [];

    if (operation.parameters) {
      operation.parameters.forEach(param => {
        let sourceType = 'RequestParam';
        if (param.in === 'path') sourceType = 'PathVariable';
        else if (param.in === 'header') sourceType = 'RequestHeader';

        parameters.push({
          name: param.name,
          type: param.schema?.type || 'string',
          isRequired: param.required || false,
          sourceType: sourceType,
          description: param.description || null
        });
      });
    }

    if (operation.requestBody) {
      const content = operation.requestBody.content?.['application/json'];
      if (content && content.schema) {
        parameters.push({
          name: 'body',
          type: content.schema,
          isRequired: operation.requestBody.required || false,
          sourceType: 'RequestBody',
          description: operation.requestBody.description || null
        });
      }
    }

    return {
      path: path,
      httpMethod: method.toUpperCase(),
      summary: operation.summary || null,
      description: operation.description || null,
      parameters: parameters,
      responses: operation.responses || {}
    };
  };

  const generateExamples = async () => {
    if (!currentEndpoint) return;

    setGeneratingExamples(true);
    setExamples([]);

    try {
      const { path, method } = currentEndpoint;
      const operation = apiSpec.paths[path][method];
      const endpointInfo = buildEndpointInfo(path, method, operation);

      const response = await fetch('/autodocer/ai/generate-examples', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(endpointInfo)
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      setExamples(data || []);
    } catch (err) {
      console.error('Error generating examples:', err);
      setExamples([{ error: err.message }]);
    } finally {
      setGeneratingExamples(false);
    }
  };

  const copyToClipboard = async (text, index) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedIndex(index);
      setTimeout(() => setCopiedIndex(null), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
      alert('Failed to copy to clipboard');
    }
  };

  const selectEndpoint = (path, method) => {
    setCurrentEndpoint({ path, method });
    setExamples([]);
    if (window.innerWidth < 768) {
      setSidebarOpen(false);
    }
  };

  const methodColors = {
    get: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-300',
    post: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
    put: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300',
    delete: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
    patch: 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-300'
  };

  return (
    <div className={`min-h-screen flex ${darkMode ? 'dark' : ''}`}>
      <style>{`
        .dark { color-scheme: dark; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; }
      `}</style>

      {/* Sidebar */}
      <aside className={`
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
        fixed md:relative z-50 w-80 h-screen bg-white dark:bg-gray-900 
        border-r border-gray-200 dark:border-gray-700 
        transition-transform duration-300 ease-in-out
        flex flex-col
      `}>
        {/* Header */}
        <div className="p-5 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
              📚 AutoDocER API
            </h1>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setDarkMode(!darkMode)}
                className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                aria-label="Toggle dark mode"
              >
                {darkMode ? <Sun className="w-5 h-5 text-gray-600 dark:text-gray-400" /> : <Moon className="w-5 h-5 text-gray-600" />}
              </button>
              <button
                onClick={() => setSidebarOpen(false)}
                className="md:hidden p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
              >
                <X className="w-5 h-5 text-gray-600 dark:text-gray-400" />
              </button>
            </div>
          </div>

          {/* Server Selector */}
          {apiSpec && (
            <div className="mb-4">
              <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                Server
              </label>
              <select
                value={selectedServer || ''}
                onChange={(e) => setSelectedServer(e.target.value)}
                className="w-full px-3 py-2 text-sm bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
              >
                {apiSpec.servers && apiSpec.servers.length > 0 ? (
                  apiSpec.servers.map((server, idx) => (
                    <option key={idx} value={server.url}>
                      {server.description || server.url}
                    </option>
                  ))
                ) : (
                  <option value="/">Default Server (/)</option>
                )}
              </select>
              <div className="mt-2 px-3 py-2 bg-gray-100 dark:bg-gray-800 rounded-lg text-xs font-mono text-gray-600 dark:text-gray-400 break-all">
                {selectedServer}
              </div>
            </div>
          )}

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search endpoints..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-3 py-2 text-sm bg-gray-50 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-900 dark:text-gray-100 placeholder-gray-500 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-3">
          {loading ? (
            <div className="flex items-center justify-center py-8 text-gray-500">
              <Loader className="w-5 h-5 animate-spin mr-2" />
              Loading...
            </div>
          ) : error ? (
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-800 dark:text-red-300 text-sm">
              <AlertTriangle className="w-4 h-4 inline mr-2" />
              Error: {error}
            </div>
          ) : (
            Object.keys(filteredEndpoints).sort().map(tag => (
              <details key={tag} open className="mb-2">
                <summary className="px-3 py-2 cursor-pointer font-semibold text-sm text-gray-900 dark:text-white rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center gap-2 select-none">
                  <ChevronRight className="w-4 h-4 transition-transform" />
                  {tag}
                </summary>
                <ul className="mt-1 ml-2 space-y-1">
                  {filteredEndpoints[tag].map(({ path, method, operation }) => (
                    <li
                      key={`${method}-${path}`}
                      onClick={() => selectEndpoint(path, method)}
                      className={`
                        px-3 py-2 rounded-lg cursor-pointer text-sm flex items-center gap-2
                        transition-all hover:bg-gray-100 dark:hover:bg-gray-800
                        ${currentEndpoint?.path === path && currentEndpoint?.method === method
                          ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 font-medium'
                          : 'text-gray-700 dark:text-gray-300'
                        }
                      `}
                    >
                      <span className={`px-2 py-1 rounded text-xs font-bold uppercase ${methodColors[method]}`}>
                        {method}
                      </span>
                      <span className="font-mono text-xs truncate">{path}</span>
                    </li>
                  ))}
                </ul>
              </details>
            ))
          )}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto bg-gray-50 dark:bg-gray-950">
        {/* Mobile Header */}
        <div className="md:hidden sticky top-0 z-40 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 p-4">
          <button
            onClick={() => setSidebarOpen(true)}
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <Menu className="w-6 h-6 text-gray-600 dark:text-gray-400" />
          </button>
        </div>

        <div className="max-w-5xl mx-auto p-6 md:p-10">
          {!currentEndpoint ? (
            <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl p-16 text-center">
              <div className="text-gray-400 dark:text-gray-600 text-lg">
                👈 Select an endpoint from the sidebar to view its documentation
              </div>
            </div>
          ) : (
            <EndpointDetails
              apiSpec={apiSpec}
              currentEndpoint={currentEndpoint}
              methodColors={methodColors}
              generatingExamples={generatingExamples}
              examples={examples}
              generateExamples={generateExamples}
              copyToClipboard={copyToClipboard}
              copiedIndex={copiedIndex}
              darkMode={darkMode}
            />
          )}
        </div>
      </main>

      {/* Overlay for mobile */}
      {sidebarOpen && (
        <div
          className="md:hidden fixed inset-0 bg-black/50 z-40"
          onClick={() => setSidebarOpen(false)}
        />
      )}
    </div>
  );
};

// Endpoint Details Component
const EndpointDetails = ({ 
  apiSpec, 
  currentEndpoint, 
  methodColors, 
  generatingExamples, 
  examples, 
  generateExamples, 
  copyToClipboard, 
  copiedIndex,
  darkMode 
}) => {
  const { path, method } = currentEndpoint;
  const operation = apiSpec.paths[path][method];

  return (
    <div className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl p-8 space-y-8">
      {/* Header */}
      <div className="flex items-center gap-3 flex-wrap">
        <span className={`px-4 py-2 rounded-lg text-sm font-bold uppercase ${methodColors[method]}`}>
          {method}
        </span>
        <span className="text-2xl font-semibold text-gray-900 dark:text-white font-mono">
          {path}
        </span>
      </div>

      {operation.summary && (
        <p className="text-gray-700 dark:text-gray-300 font-medium">{operation.summary}</p>
      )}
      {operation.description && (
        <p className="text-gray-600 dark:text-gray-400">{operation.description}</p>
      )}

      {/* Parameters */}
      <Section title="Parameters">
        {operation.parameters && operation.parameters.length > 0 ? (
          <div className="space-y-3">
            {operation.parameters.map((param, idx) => (
              <ParameterCard key={idx} param={param} darkMode={darkMode} />
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 rounded-lg">
            <Inbox className="w-8 h-8 mx-auto mb-2 opacity-50" />
            No parameters required for this endpoint
          </div>
        )}
      </Section>

      {/* Request Body */}
      {operation.requestBody && (
        <Section title="Request Body">
          <div className="flex items-center gap-2 mb-3">
            {operation.requestBody.required ? (
              <span className="px-3 py-1 text-xs font-semibold bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 rounded">
                REQUIRED
              </span>
            ) : (
              <span className="px-3 py-1 text-xs font-semibold bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-300 rounded">
                OPTIONAL
              </span>
            )}
          </div>
          {operation.requestBody.content?.['application/json']?.schema && (
            <SchemaDisplay schema={operation.requestBody.content['application/json'].schema} darkMode={darkMode} />
          )}
        </Section>
      )}

      {/* Responses */}
      {operation.responses && (
        <Section title="Responses">
          <div className="space-y-4">
            {Object.entries(operation.responses).map(([code, response]) => (
              <ResponseBlock key={code} code={code} response={response} darkMode={darkMode} />
            ))}
          </div>
        </Section>
      )}

      {/* Try It Out */}
      <div className="pt-6 border-t-2 border-gray-200 dark:border-gray-700 text-center">
        <button
          onClick={generateExamples}
          disabled={generatingExamples}
          className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white font-semibold rounded-lg shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none"
        >
          {generatingExamples ? (
            <>
              <Loader className="w-5 h-5 animate-spin" />
              Generating examples...
            </>
          ) : (
            <>
              <Code className="w-5 h-5" />
              Try It Out
            </>
          )}
        </button>

        {/* Examples Display */}
        {examples.length > 0 && (
          <div className="mt-6 space-y-4 text-left">
            {examples.map((example, idx) => (
              example.error ? (
                <div key={idx} className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-800 dark:text-red-300 text-sm">
                  <AlertTriangle className="w-4 h-4 inline mr-2" />
                  Error: {example.error}
                </div>
              ) : (
                <div key={idx} className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:border-blue-500 dark:hover:border-blue-400 transition-colors">
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                      <Code className="w-4 h-4" />
                      {example.description || `Example ${idx + 1}`}
                    </span>
                    <button
                      onClick={() => copyToClipboard(example.command, idx)}
                      className={`flex items-center gap-2 px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                        copiedIndex === idx
                          ? 'bg-green-500 text-white'
                          : 'bg-blue-500 text-white hover:bg-blue-600'
                      }`}
                    >
                      {copiedIndex === idx ? (
                        <>
                          <Check className="w-4 h-4" />
                          Copied!
                        </>
                      ) : (
                        <>
                          <Copy className="w-4 h-4" />
                          Copy
                        </>
                      )}
                    </button>
                  </div>
                  <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg text-sm overflow-x-auto font-mono">
                    {example.command}
                  </pre>
                </div>
              )
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

const Section = ({ title, children }) => (
  <div>
    <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 pb-2 border-b border-gray-200 dark:border-gray-700">
      {title}
    </h2>
    {children}
  </div>
);

const ParameterCard = ({ param, darkMode }) => (
  <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:border-blue-500 dark:hover:border-blue-400 transition-colors">
    <div className="flex items-center justify-between flex-wrap gap-2 mb-2">
      <div className="flex items-center gap-2">
        <span className="font-mono font-semibold text-blue-600 dark:text-blue-400">{param.name}</span>
        <span className="px-2 py-1 text-xs bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded border border-gray-300 dark:border-gray-600">
          {param.in}
        </span>
      </div>
      <div className="flex items-center gap-2">
        <span className="font-mono text-sm text-green-600 dark:text-green-400 font-medium">
          {param.schema?.type || 'any'}
        </span>
        {param.required ? (
          <span className="px-2 py-1 text-xs font-semibold bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 rounded">
            REQUIRED
          </span>
        ) : (
          <span className="px-2 py-1 text-xs font-semibold bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-300 rounded">
            OPTIONAL
          </span>
        )}
      </div>
    </div>
    {param.description && (
      <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">{param.description}</p>
    )}
    {param.schema && <ConstraintsDisplay schema={param.schema} />}
  </div>
);

const ConstraintsDisplay = ({ schema }) => {
  const constraints = [];
  if (schema.minLength !== undefined) constraints.push(`Min Length: ${schema.minLength}`);
  if (schema.maxLength !== undefined) constraints.push(`Max Length: ${schema.maxLength}`);
  if (schema.minimum !== undefined) constraints.push(`Min: ${schema.minimum}`);
  if (schema.maximum !== undefined) constraints.push(`Max: ${schema.maximum}`);
  if (schema.pattern) constraints.push(`Pattern: ${schema.pattern}`);
  if (schema.enum) constraints.push(`Enum: [${schema.enum.join(', ')}]`);

  if (constraints.length === 0) return null;

  return (
    <div className="mt-2 p-2 bg-white dark:bg-gray-900 rounded border-l-4 border-orange-500 text-xs text-gray-600 dark:text-gray-400">
      {constraints.map((c, i) => (
        <span key={i} className="mr-3">{c}</span>
      ))}
    </div>
  );
};

const SchemaDisplay = ({ schema, depth = 0 }) => {
  if (!schema) return <span className="text-gray-500">No schema</span>;

  return (
    <pre className="bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg p-4 text-sm font-mono overflow-x-auto text-gray-900 dark:text-gray-100">
      {renderSchema(schema, depth)}
    </pre>
  );
};

const renderSchema = (schema, depth = 0, required = []) => {
  if (!schema) return 'any';
  
  const indent = '  '.repeat(depth);
  
  if (schema.type === 'object' && schema.properties) {
    let result = '{\n';
    const props = Object.entries(schema.properties);
    const requiredFields = schema.required || required;
    
    props.forEach(([name, prop], idx) => {
      const isRequired = requiredFields.includes(name);
      const reqMark = isRequired ? '*' : '';
      result += `${indent}  "${name}"${reqMark}: `;
      
      if (prop.type && prop.type !== 'object' && prop.type !== 'array') {
        result += prop.type;
        if (prop.format) result += `<${prop.format}>`;
      } else {
        result += renderSchema(prop, depth + 1, requiredFields);
      }
      
      if (idx < props.length - 1) result += ',';
      result += '\n';
    });
    result += `${indent}}`;
    return result;
  } else if (schema.type === 'array' && schema.items) {
    return `[\n${indent}  ${renderSchema(schema.items, depth + 1)}\n${indent}]`;
  } else {
    return schema.type || 'any';
  }
};

const ResponseBlock = ({ code, response, darkMode }) => (
  <div className="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg p-4">
    <h3 className="font-semibold text-gray-900 dark:text-white mb-2 flex items-center gap-2">
      <CheckCircle className="w-5 h-5 text-green-500" />
      Status {code}
    </h3>
    <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
      {response.description || 'No description'}
    </p>
    {response.content?.['application/json']?.schema && (
      <SchemaDisplay schema={response.content['application/json'].schema} />
    )}
  </div>
);

export default AutoDocerUI;