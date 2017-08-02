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

    network.on("click", function(params) {
      clickAction(params);
    });

    function clickAction(params) {
      if (params.nodes.length == 1) {
        var node = getItem(params.nodes[0], data.nodes);

        var programState = "";
        if (node) {
          programState = getProgramState(node);
        }
        detailsPanels['node']
          .find('.panel-body')
          .html(programState);
        // TODO add program points and link to siblings

        detailsPanels['info'].hide();
        detailsPanels['node'].show();
        detailsPanels['edge'].hide();
      } else if (params.edges.length == 1) {
        var edge = getItem(params.edges[0], data.edges);
        // TODO add edge INFO
        detailsPanels['info'].hide();
        detailsPanels['node'].hide();
        detailsPanels['edge'].show();
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

    function getProgramState(node) {
      var result = '<h3>Program State</h3>';
      result += '<table>';
      result += tableLine('Values', node.psValues);
      result += tableLine('Constraints', node.psConstraints);
      result += tableLine('Stack', stackItem(node.psStack));
      // TODO add all possible yields
      result += '</table>';

      return result;
    }

    function tableLine(label, value) {
      if (value) {
        var newValue = value;
        if (typeof value == 'string'  && value.startsWith('{') && value.endsWith('}')) {
          var valueAsObject = JSON.parse(value.replace(/\?/g, '"'));
          newValue = '<table class="innerTable">';
          for (var key in valueAsObject) {
            newValue += tableLine(key, valueAsObject[key]);
          }
          newValue += '</table>'
        }
        return '<tr><td>' + label + '</td><td>' + newValue + '</td></tr>';
      }
      return "";
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
  }
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
