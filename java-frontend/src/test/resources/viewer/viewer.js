function loadEG(DOTstring, displayAsTree) {
  loadDot(DOTstring, true, displayAsTree);
};

function loadSyntaxTree(DOTstring) {
  loadDot(DOTstring, false, true);
};

function loadCFG(DOTstring) {
  loadDot(DOTstring, false, false);
};

function loadDot(DOTstring, useProgramStates, displayAsTree) {
  var DEFAULT_PROGRAM_STATE_HTML= '<h3>Program State:</h3><em>Select a node from the graph to display its program state...</em>';

  if (!useProgramStates) {
    removeProgramStateDiv();
  }

  var parsedData = vis.network.convertDot(DOTstring);

  var data = {
    nodes: parsedData.nodes,
    edges: parsedData.edges
  }

  var options = parsedData.options;

  options.nodes = {
    color: {
      background: '#eee',
      border: 'gray',
      highlight:{
        background: 'yellow',
        border: 'gold'
      }
    },
    font: {
      size: 12,
      face: 'monospace',
      color: '#333',
      align: 'left'
    }
  }
  if(displayAsTree) {
    options.layout = {
      hierarchical: {
        enabled: true,
        sortMethod: 'directed',
        levelSeparation: 100,
        nodeSpacing: 1
      }
    }
  }

  options.edges = {
    font: {
      color: 'grey',
      size: '10'
    }
  }

  var network = new vis.Network(container, data, options);

  if (useProgramStates) {
    network.on("click", function(params) {
      document.getElementById('programstate').innerHTML = clickAction(params);
    });

    function clickAction(params) {
      var result = DEFAULT_PROGRAM_STATE_HTML;
      if (params.nodes.length == 1) {
        result = getProgramState(params.nodes[0]);
      } else if (params.edges.length == 1) {
        result = getYield(params.edges[0]);
      }
      return result;
    }

    function getProgramState(nodeId) {
      var stackAsString, constraintsAsString, valuesAsString, lastEvaluatedSymbolAsString;
      data.nodes.forEach(function(node) {
        if (nodeId == node.id) {
          valuesAsString = node.psValues;
          constraintsAsString = node.psConstraints;
          stackAsString = node.psStack;
          lastEvaluatedSymbolAsString = node.psLastEvaluatedSymbol;
        }
      });

      result = '<h3>Program State:</h3>';
      result += '<table>';
      result += tableLine('values', valuesAsString);
      result += tableLine('constraints', constraintsAsString);
      result += tableLine('stack', stackAsString);
      result += tableLine('lastEvaluatedSymbol', lastEvaluatedSymbolAsString);
      result += '</table>';

      return result;
    }

    function getYield(edgeId) {
      var selectedMethodYieldAsString;
      data.edges.forEach(function(edge) {
        if (edgeId == edge.id) {
          selectedMethodYieldAsString = edge.selectedMethodYield;
        }
      });

      if (selectedMethodYieldAsString) {
        result = '<h3>Selected Method Yield:</h3>';
        result += '<code>' + selectedMethodYieldAsString + '</code>';
      }
      return result;
    }

    function tableLine(label, value) {
      if (value) {
        return '<tr><td>' + label + '</td><td>' + value + '</td></tr>';
      }
      return "";
    }
  }
};

function removeProgramStateDiv() {
  var programStateDiv = document.getElementById('programstate');
  if (programStateDiv) {
    programStateDiv.parentNode.removeChild(programStateDiv);
  }
};
