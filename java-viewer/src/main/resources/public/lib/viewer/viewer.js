function loadDot(DOTstring, targetContainer, displayAsTree) {
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

  new vis.Network(targetContainer, data, options);
};

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
