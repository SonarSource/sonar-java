function loadDot(DOTstring, targetContainer, hierarchical, detailsPanels) {
  var parsedData = vis.network.convertDot(DOTstring);

  var data = {
    nodes: new vis.DataSet(parsedData.nodes),
    edges: new vis.DataSet(parsedData.edges)
  }

  setNodesColor(data.nodes);
  setEdgesColor(data.edges);

  var network = new vis.Network(targetContainer, data);
  changeLayout(network, hierarchical);

  if (detailsPanels) {
    // by default only show info panel
    detailsPanels['info'].show();
    detailsPanels['node'].hide();
    detailsPanels['edge'].hide();

    var ppMap = getPPMap(data.nodes);
    // enrich network with mapping between PP and nodes
    network['eg'] = {
      'ppMap' : ppMap,
      'nodes' : data.nodes
    };

    network.on('click', function(params) {
      clickAction(params);
    });

    function clickAction(params) {
      // reset any custom color from selection
      setNodesColor(data.nodes);

      var nodeHtmlContent = '<p>No data</p>';

      if (params.nodes.length == 1) {
        var node = getItem(params.nodes[0], data.nodes);

        if (node) {
          nodeHtmlContent = getNodeDetails(node);
          var ppKey = node.ppKey;
          detailsPanels['node'].find('#ppKey').html(ppKey);
          var nodeIdsWithSamePP = ppMap[ppKey];
          if (nodeIdsWithSamePP) {
            var sameProgramPointBtn = detailsPanels['node'].find('#same-pp-btn');
            var disabled = nodeIdsWithSamePP.length <= 1;
            if (disabled) {
              sameProgramPointBtn.removeClass('btn-primary');
              sameProgramPointBtn.addClass('btn-default');
            } else {
              sameProgramPointBtn.addClass('btn-primary');
              sameProgramPointBtn.removeClass('btn-default');
            }
            // highlight given node
            highlightAllNodesAtSamePP(ppKey, [node.id], data.nodes, network);

            // enable button and handlers
            sameProgramPointBtn.attr('disabled', disabled);
            sameProgramPointBtn.unbind('click', highlightAllNodesAtSamePP);
            sameProgramPointBtn.on('click', function (e) {
              highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, data.nodes, network);
            });
          }
        }

        detailsPanels['info'].hide();
        detailsPanels['node'].find('#nodeDetails-content').html(nodeHtmlContent);
        detailsPanels['node'].show();
        detailsPanels['node'].find('.collapse').collapse('show');
        detailsPanels['edge'].hide();
      } else if (params.edges.length == 1) {
        var edge = getItem(params.edges[0], data.edges);

        if (edge) {
          nodeHtmlContent = getEdgeDetails(edge);
        }

        detailsPanels['info'].hide();
        detailsPanels['node'].hide();
        detailsPanels['edge'].find('#edgeDetails-content').html(nodeHtmlContent);
        detailsPanels['edge'].show();
        detailsPanels['edge'].find('.collapse').collapse('show');
      } else {
        detailsPanels['info'].hide();
        detailsPanels['node'].hide();
        detailsPanels['edge'].hide();
      }
    }

    function getItem(itemId, collection) {
      var result = null;
      collection.forEach(function(item) {
        if (itemId == item.id) {
          result = item;
          return;
        }
      });
      return result;
    }

    function getNodeDetails(node) {
      var result = '<h3>Program State</h3>';
      result += getProgramState(node);

      if (node.methodYields) {
        result += '<hr>';
        result += '<h3>Method Yields: ' + node.methodName + '(...)</h3>';
        result += getMethodYields(node.methodYields);
      }

      return result;
    }

    function getProgramState(node) {
      var result = '<table class="programState-table">';
      result += tableLine('Values', node.psValues);
      result += tableLine('Constraints', node.psConstraints);
      result += tableLine('Stack', stackItem(node.psStack));
      result += '</table>';
      return result;
    }

    function tableLine(label, value) {
      if (value) {
        var newValue = value;
        if (typeof value == 'string'  && value.startsWith('{') && value.endsWith('}')) {
          var valueAsObject = asJsonObject(value);
          newValue = '<table class="innerTable">';
          for (var key in valueAsObject) {
            newValue += tableLine(key, valueAsObject[key]);
          }
          newValue += '</table>'
        }
        return '<tr><td>' + label + '</td><td>' + newValue + '</td></tr>';
      }
      return '';
    }

    function stackItem(value) {
      var result = '<ul class="list-group">';
      if (value == '{}') {
        result += '<li class="list-group-item"><em>empty</em></li>';
      } else {
        var valueAsObject = asJsonObject(value);
        for (var key in valueAsObject) {
          var item = valueAsObject[key];
          if (item == 'null') {
            item = '';
          }
          result += '<li class="list-group-item">' + key + '<sub>' + item + '</sub></li>';
        }
      }
      result += '</ul>';

      return result;
    }

    function getMethodYields(methodYieldsAsString) {
      var result = '<table class="methodYield-table">';
      result += '<tr>';
      result += '<th></th>';
      result += '<th>Parameters constraints</th>';
      result += '<th>Result constraint / Thrown exception</th>';
      result += '</tr>';
      var methodYieldsAsObject = asJsonObject(methodYieldsAsString);
      var methodYieldNumber = 1;
      for (var methodYieldId in methodYieldsAsObject) {
        result += getMethodYield(methodYieldsAsObject[methodYieldId], methodYieldNumber);
        methodYieldNumber++;
      }
      result += '</table>';
      return result;
    }

    function getMethodYield(methodYield, id) {
      var result = '<tr>';
      if (id) {
        result += '<td>#' + id + '</td>';
      }
      result += '<td>' + methodParameters(methodYield.params) + '</td>'
      result += '<td>' + methodResult(methodYield.result, methodYield.resultIndex, methodYield.exception) + '</td>';
      result += '</tr>';
      return result;
    }

    function methodParameters(params) {
      var result = '<table class="innerTable">';
      for (var paramId in params) {
        result += '<tr><td>arg'+ paramId +'</td><td>' + params[paramId] + '</td></tr>'
      }
      result += '</table>';
      return result;
    }

    function methodResult(resultConstraints, resultIndex, exception) {
      if (exception) {
        return '<code>' + exception + '</code>';
      }
      var result = resultConstraints;
      if (resultIndex != -1) {
        result += ' (arg' + resultIndex + ')';
      }
      return result;
    }

    function getEdgeDetails(edge) {
      if (!edge.learnedConstraints && !edge.learnedAssociations && ! edge.selectedMethodYield) {
        return '<em>No data...</em>';
      }
      var result = '';
      if (edge.learnedConstraints) {
        result += '<h3>Learned constraints</h3>';
        result += '<table>';
        var learnedConstraintsAsObject = asJsonObject(edge.learnedConstraints);
        for (var lc in learnedConstraintsAsObject) {
          result += tableLine(lc, learnedConstraintsAsObject[lc]);
        }
        result += '</table>';
      }
      if (edge.learnedAssociations) {
        if (edge.learnedConstraints) {
          result += '<hr>';
        }
        result += '<h3>Learned associations</h3>';
        result += '<table>';
        var learnedAssociationsAsObject = asJsonObject(edge.learnedAssociations);
        for (var la in learnedAssociationsAsObject) {
          result += tableLine(la, learnedAssociationsAsObject[la]);
        }
        result += '</table>';
      }
      if (edge.selectedMethodYield) {
        if (edge.learnedAssociations || edge.learnedConstraints) {
          result += '<hr>';
        }
        result += '<h3>Method Yield</h3>';
        var methodYieldAsObject = asJsonObject(edge.selectedMethodYield);
        result += '<table class="methodYield-table">';
        result += '<tr>';
        result += '<th>Parameters constraints</th>';
        result += '<th>Result constraint / Thrown exception</th>';
        result += '</tr>';
        result += getMethodYield(methodYieldAsObject);
        result += '</table>';
      }
      return result;
    }

    function asJsonObject(value) {
      return JSON.parse(value.replace(/\?/g, '"'));
    }
  }

  return network;
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

    var highlighting = $.inArray(node.id, selectedNodesIds) != -1 ? forcedHighlighting : node.highlighting;

    switch(highlighting) {
      case 'firstNode':
        node['color']['background'] = 'palegreen';
        node['color']['border'] = 'limegreen';
        break;
      case 'exitNode':
        node['color']['background'] = 'black';
        node['color']['border'] = 'dimgray';
        node['font']['color'] = 'white';
        break;
      case 'lostNode':
        node['color']['background'] = 'red';
        node['color']['border'] = 'firebrick';
        node['font']['color'] = 'white';
        break;
      case 'tokenKind':
        node['color']['background'] = 'black';
        node['color']['border'] = 'dimgray';
        node['font']['color'] = 'white';
        node['shape'] = 'box';
        break;
      case 'classKind':
        node['color']['background'] = 'pink';
        node['color']['border'] = 'red';
        break;
      case 'methodKind':
        node['color']['background'] = 'skyblue';
        node['color']['border'] = 'blue';
        break;
      case 'samePP':
        node['color']['background'] = 'pink';
        node['color']['border'] = 'mediumvioletred';
        node['font']['color'] = 'black';
        break;
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
    }

    edges.update(edge);
  });
}

function getPPMap(nodes) {
  var result = {};
  nodes.forEach(function (node) {
    if (node.ppKey) {
      if (!result[node.ppKey]) {
        result[node.ppKey] = [];
      }
      result[node.ppKey].push(node.id);
    }
  });
  return result;
}

function mapPPByLine(cfgCode) {
  var result = {};
  var currentBlock;
  var lines = cfgCode.split('\n');
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (line.startsWith('B') && !line.startsWith('B0')) {
      currentBlock = line;
    } else if (line.startsWith('B0')) {
      result['B0.0'] = i;
    } else if (line.match(/^\d/)) {
      var blockElement = currentBlock + '.' + line.split(':')[0];
      result[blockElement] = i;
    }
  }
  return result;
}

function highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, nodes, network) {
  var samePPNodes = [];
  nodes.forEach(function (node) {
    if ($.inArray(node.id, nodeIdsWithSamePP) != -1) {
      samePPNodes.push(node.id);
    }
  });

  // update CFG editor line
  var cfgEditorLine = network['eg']['ppMapCFG'][ppKey];
  if (cfgEditorLine) {
    network['eg']['cfgEditorSelectedLine'] = cfgEditorLine;
    network['eg']['cfgEditor'].setCursor(cfgEditorLine);
  }

  // update graph
  setNodesColor(nodes, samePPNodes, 'samePP');
  network.redraw();
}

function handleNewPP(editor, network) {
  var newLine = editor.getCursor()['line'];

  if (network['eg']['cfgEditorSelectedLine'] == newLine) {
    // already on that line, nothing to do
    return;
  }

  network['eg']['cfgEditorSelectedLine'] = newLine;

  var ppMapCFG = network['eg']['ppMapCFG'];
  var ppKey = null;
  for(var key in ppMapCFG) {
    var ppMapLine = ppMapCFG[key];
    if (newLine == ppMapLine) {
      ppKey = key;
      break;
    }
  }

  var nodeIdsWithSamePP = [];
  if (ppKey) {
    nodeIdsWithSamePP = network['eg']['ppMap'][ppKey];
  }
  highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, network['eg']['nodes'], network);
}

function changeLayout(network, hierarchical) {
  var options = { 'layout' : { 'hierarchical' : false } }
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
