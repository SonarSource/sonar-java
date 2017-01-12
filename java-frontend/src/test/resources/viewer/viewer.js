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
    network.on("selectNode", function(params) {
      document.getElementById('programstate').innerHTML = getProgramState(params.nodes);
    });

    network.on("deselectNode", function(params) {
      document.getElementById('programstate').innerHTML = DEFAULT_PROGRAM_STATE_HTML;
    });

    function getProgramState(nodeIds) {
      var result = DEFAULT_PROGRAM_STATE_HTML;
      if (nodeIds.length == 1) {
        var nodeId = nodeIds[0];
        var programStateAsString;
        data.nodes.forEach(function(node) {
          if (nodeId == node.id) {
            programStateAsString = node.programState;
          }
        })

        // ugly hack to get differents parts of the program state based on its toString() method. 
        // Should be refactored in order to get correctly each object
        var groups = programStateAsString.split('{');
        if (groups.length == 5) {
          result = '<h3>Program State:</h3>';
          result += '<table>';
          result += tableLine('values',groups[1]);
          result += tableLine('constraints', groups[2]);
          result += tableLine('stack', groups[3]);
          result += tableLine('lastEvaluatedSymbol', groups[4]);
          result += '</table>';
        }
      }
      return result;
    }

    function tableLine(label, value) {
      return '<tr><td>' + label + '</td><td>' + clean(value) + '</td></tr>';
    }

    function clean(value) {
      return value.substring(0, value.indexOf('}'));
    }
  }
};

function removeProgramStateDiv() {
  var programStateDiv = document.getElementById('programstate');
  if (programStateDiv) {
    programStateDiv.parentNode.removeChild(programStateDiv);
  }
};
