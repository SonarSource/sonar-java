function loadDot(DOTstring, targetContainer, hierarchical, detailsPanels) {
  const parsedData = vis.network.convertDot(DOTstring);

  const data = {
    nodes: new vis.DataSet(parsedData.nodes),
    edges: new vis.DataSet(parsedData.edges)
  };

  updateDetails(data.nodes);
  updateDetails(data.edges);

  setNodesColor(data.nodes);
  setEdgesColor(data.edges);

  let network = new vis.Network(targetContainer, data);
  changeLayout(network, hierarchical);

  if (detailsPanels) {
    // by default only show info panel
    detailsPanels['info'].show();
    detailsPanels['node'].hide();
    detailsPanels['edge'].hide();

    const ppMap = getPPMap(data.nodes);
    // enrich network with mapping between PP and nodes
    network['eg'] = {
      'ppMap' : ppMap,
      'nodes' : data.nodes
    };

    network.on('click', function(params) {
      clickAction(params, data, network, detailsPanels);
    });
  }

  return network;
}

function clickAction(params, data, network, detailsPanels) {
  // reset any custom color from selection
  setNodesColor(data.nodes);

  if (params.nodes.length === 1) {
    const node = data.nodes.get(params.nodes[0]);

    if (node) {
      const nodeHtmlContent = getNodeDetails(node['details']);

      detailsPanels['info'].hide();
      detailsPanels['node'].find('#nodeDetails-content').html(nodeHtmlContent);
      detailsPanels['node'].show();
      detailsPanels['node'].find('.collapse').collapse('show');
      detailsPanels['edge'].hide();
      return;
    }

  } else if (params.edges.length === 1) {
    const edge = data.edges.get(params.edges[0]);

    if (edge) {
      const nodeHtmlContent = getEdgeDetails(edge['details']);

      detailsPanels['info'].hide();
      detailsPanels['node'].hide();
      detailsPanels['edge'].find('#edgeDetails-content').html(nodeHtmlContent);
      detailsPanels['edge'].show();
      detailsPanels['edge'].find('.collapse').collapse('show');
      return;
    }
  }

  detailsPanels['info'].hide();
  detailsPanels['node'].hide();
  detailsPanels['edge'].hide();
}

function updateDetails(collection) {
  collection.forEach(function (item) {
    let changed = false;
    const label = item['label'];
    if (label) {
      item['label'] = unescapeSpecialChars(item['label']);
      changed = true;
    }
    const details = item['details'];
    if (details) {
      item['details'] = JSON.parse(details.replace(/\?/g, '"'));
      changed = true;
    }
    if (changed) {
      collection.update(item);
    }
  });
}

function unescapeSpecialChars(text) {
  let result = text;
  result = result.replace(/&quest;/g, '?');
  result = result.replace(/&quot;/g, '"');
  return result;
}

function getNodeDetails(details) {
  if (!details) {
    return '<em>No data...</em>';
  }
  let result = '<h3>Program State</h3>';
  result += getProgramState(details);

  if (details.methodYields) {
    result += '<hr>';
    result += `<h3>Method Yields: ${details.methodName}(...)</h3>`;
    result += getMethodYields(details.methodYields);
  }

  return result;
}

function getProgramState(details) {
  let result = '';
  if (details.psValues) {
    result += tableLine('Values', getValues(details.psValues));
  }
  if (details.psConstraints) {
    result += tableLine('Constraints', getConstraints(details.psConstraints));
  }
  if (details.psStack) {
    result += tableLine('Stack', getStack(details.psStack));
  }
  return table(result, 'programState-table');
}

function getMethodYields(methodYields) {
  let result = '<tr>';
  result += '<th></th>';
  result += '<th>Parameters constraints</th>';
  result += '<th>Result constraint / Thrown exception</th>';
  result += '</tr>';
  let methodYieldNumber = 1;
  methodYields.forEach(function (methodYield) {
    result += getMethodYield(methodYield, methodYieldNumber);
    methodYieldNumber++;
  });
  return table(result, 'methodYield-table');
}

function getMethodYield(methodYield, id) {
  let result = '';
  result += tableCell(`#${id}`);
  result += tableCell(methodParameters(methodYield.params));
  result += tableCell(methodResult(methodYield.result, methodYield.resultIndex, methodYield.exception));
  return tableRow(result);
}

function methodParameters(params) {
  let result = '';
  for (let i = 0; i < params.length; i++) {
    result += tableLine(`arg${i}`, params[i]);
  }
  return table(result, 'innerTable');
}

function methodResult(resultConstraints, resultIndex, exception) {
  if (exception) {
    return `<code>${exception}</code>`;
  }
  let result = resultConstraints;
  if (resultIndex !== -1) {
    result += ` (arg${resultIndex})`;
  }
  return result;
}

function table(content, style) {
  let classStyle = '';
  if (style) {
    classStyle = ` class="${style}"`;
  }
  return `<table${classStyle}>${content}</table>`;
}

function tableCell(value) {
  return `<td>${value}</td>`;
}

function tableRow(value) {
  return `<tr>${value}</tr>`;
}

function tableLine(label, value) {
  if (value) {
    const c1 = tableCell(label);
    const c2 = tableCell(value);
    return tableRow(c1 + c2);
  }
  return '';
}

function getValues(values) {
  let result = '';
  values.forEach(function (value) {
    result += tableLine(value['symbol'], value['sv']);
  });
  return table(result, 'innerTable');
}

function getConstraints(constraints) {
  let result = '';
  constraints.forEach(function (constraint) {
    result += tableLine(constraint['sv'], constraint['constraints']);
  });
  return table(result, 'innerTable');
}

function getStack(items) {
  let result = '<ul class="list-group">';
  if (items.length === 0) {
    result += '<li class="list-group-item"><em>empty</em></li>';
  } else {
    items.forEach(function (item) {
      let symbol = item['symbol'];
      if (!symbol) {
        symbol = '';
      }
      result += `<li class="list-group-item">${item['sv']}<sub>${symbol}</sub></li>`;
    });
  }
  result += '</ul>';

  return result;
}

function getEdgeDetails(details) {
  if (!details || (!details.learnedConstraints && !details.learnedAssociations && !details.selectedMethodYields)) {
    return '<em>No data...</em>';
  }
  let result = '';
  if (details.learnedConstraints) {
    result += getLearnedConstraints(details.learnedConstraints);
  }
  if (details.learnedAssociations) {
    if (details.learnedConstraints) {
      result += '<hr>';
    }
    result += getLearnedAssociations(details.learnedAssociations);
  }
  if (details.selectedMethodYields) {
    if (details.learnedAssociations || details.learnedConstraints) {
      result += '<hr>';
    }
    result += '<h3>Selected method yields</h3>';
    result += getMethodYields(details.selectedMethodYields);
  }
  return result;
}

function getLearnedConstraints(lcs) {
  let result = '<h3>Learned constraints</h3>';
  result += '<table>';
  lcs.forEach(function (lc) {
    result += tableLine(lc['sv'], lc['constraint']);
  });
  result += '</table>';
  return result;
}

function getLearnedAssociations(las) {
  let result = '<h3>Learned associations</h3>';
  result += '<table>';
  las.forEach(function (la) {
    result += tableLine(la['symbol'], la['sv']);
  });
  result += '</table>';
  return result;
}

function inArray(value, array) {
  if (!array) {
    return false;
  }
  let result = false;
  array.forEach(function (item) {
    if (item === value) {
      result = true;
    }
  });
  return result;
}

function setNodesColor(nodes, selectedNodesIds, forcedHighlighting) {
  nodes.forEach(function (node) {
    // common properties
    node['color'] = {
      background: '#eee',
      border: 'gray',
      highlight: {
        background: 'yellow',
        border: 'gold'
      }
    };
    node['font'] = {
      size: 12,
      face: 'monospace',
      color: '#333',
      align: 'left'
    };

    const highlighting = inArray(node.id, selectedNodesIds) ? forcedHighlighting : node.highlighting;

    let newBackgroundColor, newBorderColor, newFontColor;
    switch(highlighting) {
      case 'firstNode':
        newBackgroundColor = 'palegreen';
        newBorderColor = 'limegreen';
        break;
      case 'exitNode':
        newBackgroundColor = 'black';
        newBorderColor = 'dimgray';
        newFontColor = 'white';
        break;
      case 'lostNode':
        newBackgroundColor = 'red';
        newBorderColor = 'firebrick';
        newFontColor = 'white';
        break;
      case 'tokenKind':
        newBackgroundColor = 'black';
        newBorderColor = 'dimgray';
        newFontColor = 'white';
        node['shape'] = 'box';
        break;
      case 'classKind':
        newBackgroundColor = 'pink';
        newBorderColor = 'red';
        break;
      case 'methodKind':
        newBackgroundColor = 'skyblue';
        newBorderColor = 'blue';
        break;
      case 'samePP':
        newBackgroundColor = 'pink';
        newBorderColor = 'mediumvioletred';
        newFontColor = 'black';
        break;
    }
    if (newBackgroundColor) {
      node['color']['background'] = newBackgroundColor;
    }
    if (newBorderColor) {
      node['color']['border'] = newBorderColor;
    }
    if (newFontColor) {
      node['font']['color'] = newFontColor;
    }
    nodes.update(node);
  });
}

function setEdgesColor(edges) {
  edges.forEach(function (edge) {
    // common properties
    edge['color'] = {
      color: 'gray',
      highlight: 'yellow'
    };
    edge['font'] = {
      size: 10,
      color: 'gray'
    };

    switch(edge.highlighting) {
      case 'exceptionEdge':
        edge['color']['color'] = 'red';
        edge['font']['color'] = 'red';
        break;
      case 'yieldEdge':
        edge['color']['color'] = 'purple';
        edge['font']['color'] = 'purple';
        break;
      default:
        // do nothing
    }

    edges.update(edge);
  });
}

function getPPMap(nodes) {
  let result = {};
  nodes.forEach(function (node) {
    const details = node['details'];
    if (details && details.ppKey) {
      if (!result[details.ppKey]) {
        result[details.ppKey] = [];
      }
      result[details.ppKey].push(node.id);
    }
  });
  return result;
}

function mapPPByLine(cfgCode) {
  let result = {};
  let currentBlock;
  let lines = cfgCode.split('\n');
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.startsWith('B') && !line.startsWith('B0')) {
      currentBlock = line;
    } else if (line.startsWith('B0')) {
      result['B0.0'] = i;
    } else if (line.match(/^\d/)) {
      let blockElement = currentBlock + '.' + line.split(':')[0];
      result[blockElement] = i;
    }
  }
  return result;
}

function highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, nodes, network) {
  let samePPNodes = [];
  nodes.forEach(function (node) {
    if (inArray(node.id, nodeIdsWithSamePP)) {
      samePPNodes.push(node.id);
    }
  });

  // update CFG editor line
  const cfgEditorLine = network['eg']['ppMapCFG'][ppKey];
  if (cfgEditorLine) {
    network['eg']['cfgEditorSelectedLine'] = cfgEditorLine;
    network['eg']['cfgEditor'].setCursor(cfgEditorLine);
  }

  // update graph
  setNodesColor(nodes, samePPNodes, 'samePP');
  network.redraw();
}

function handleNewPP(editor, network) {
  const newLine = editor.getCursor()['line'];

  if (network['eg']['cfgEditorSelectedLine'] === newLine) {
    // already on that line, nothing to do
    return;
  }

  network['eg']['cfgEditorSelectedLine'] = newLine;

  const ppMapCFG = network['eg']['ppMapCFG'];
  let ppKey = null;
  for(let key in ppMapCFG) {
    const ppMapLine = ppMapCFG[key];
    if (newLine === ppMapLine) {
      ppKey = key;
      break;
    }
  }

  let nodeIdsWithSamePP = [];
  if (ppKey) {
    nodeIdsWithSamePP = network['eg']['ppMap'][ppKey];
  }
  highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, network['eg']['nodes'], network);
}

function changeLayout(network, hierarchical) {
  let options = { 'layout' : { 'hierarchical' : false } };
  if (hierarchical) {
    options['layout']['hierarchical'] = {
        enabled: true,
        sortMethod: 'directed',
        levelSeparation: 100,
        nodeSpacing: 1
      };
  }
  network.setOptions(options);
}

// Exposes method to be tested
try {
  module.exports = {
    clickAction,
    table,
    tableLine,
    getEdgeDetails,
    getNodeDetails,
    updateDetails,
    getProgramState,
    getLearnedAssociations,
    getLearnedConstraints,
    getMethodYield,
    setNodesColor,
    setEdgesColor,
    changeLayout,
    mapPPByLine,
    getPPMap,
    highlightAllNodesAtSamePP,
    handleNewPP
  };
} catch(moduleNotDefined) {
  // NOP
}
