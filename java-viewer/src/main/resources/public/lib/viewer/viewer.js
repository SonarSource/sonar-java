function loadDot(DOTstring, targetContainer, displayAsTree, detailsPanels) {
  var parsedData = vis.network.convertDot(DOTstring);

  var data = {
    nodes: parsedData.nodes,
    edges: parsedData.edges
  }

  /* START: DEBUG STYLE */
  //  data.nodes.push({
  //    id: 12412312,
  //    label: 'lost',
  //    highlighting: 'lostNode',
  //  });
  //  data.edges.push({
  //    arrows: 'to',
  //    from: 12412312,
  //    to: 0,
  //    label: 'exception',
  //    highlighting: 'exceptionEdge',
  //  });
  //  data.edges.push({
  //    arrows: 'to',
  //    from: 12412312,
  //    to: 1,
  //    label: 'yield',
  //    highlighting: 'yieldEdge',
  //  });
  /* END: DEBUG STYLE */

  setNodesColor(data.nodes);
  setEdgesColor(data.edges);

  var options = parsedData.options;

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

  var network = new vis.Network(targetContainer, data, options);

  if (detailsPanels) {
    // by default only show info panel
    detailsPanels['info'].show();
    detailsPanels['node'].hide();
    detailsPanels['edge'].hide();

    network.on('click', function(params) {
      clickAction(params);
    });

    function clickAction(params) {
      var nodeHtmlContent = '<p>No data</p>';
      if (params.nodes.length == 1) {
        var node = getItem(params.nodes[0], data.nodes);

        if (node) {
          nodeHtmlContent = getNodeDetails(node);
        }
        // TODO add program points and link to siblings

        detailsPanels['info'].hide();
        detailsPanels['node'].find('.panel-body').html(nodeHtmlContent);
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
        detailsPanels['edge'].find('.panel-body').html(nodeHtmlContent);
        detailsPanels['edge'].show();
        detailsPanels['edge'].find('.collapse').collapse('show');
      } else {
        detailsPanels['info'].show();
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
      result += programStateTableLine('Values', node.psValues);
      result += programStateTableLine('Constraints', node.psConstraints);
      result += programStateTableLine('Stack', stackItem(node.psStack));
      result += '</table>';
      return result;
    }

    function programStateTableLine(label, value) {
      if (value) {
        var newValue = value;
        if (typeof value == 'string'  && value.startsWith('{') && value.endsWith('}')) {
          var valueAsObject = JSON.parse(value.replace(/\?/g, '"'));
          newValue = '<table class="innerTable">';
          for (var key in valueAsObject) {
            newValue += programStateTableLine(key, valueAsObject[key]);
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
        var valueAsObject = JSON.parse(value.replace(/\?/g, '"'));
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
      var methodYieldsAsObject = JSON.parse(methodYieldsAsString.replace(/\?/g, '"'));
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
      var result = '<em>No data...</em>';
      if (edge.selectedMethodYield) {
        result = '<h3>Method Yield</h3>';
        var methodYieldAsObject = JSON.parse(edge.selectedMethodYield.replace(/\?/g, '"'));
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
  }

  return network;
}

function setNodesColor(nodes) {
  for (var id in nodes) {
    var node = nodes[id];

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

    switch(node.highlighting) {
      case 'firstNode':
        node['color']['background'] = 'green';
        node['color']['border'] = 'limegreen';
        node['font']['color'] = 'white';
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
      case 'syntaxToken':
        node['color']['background'] = 'skyblue';
        node['color']['border'] = 'slateblue';
        node['shape'] = 'box';
        break;
    }
  }
}

function setEdgesColor(edges) {
  for (var id in edges) {
    var edge = edges[id];

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
  }
}
