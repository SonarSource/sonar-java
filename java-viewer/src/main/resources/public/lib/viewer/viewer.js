function loadDot(DOTstring, targetContainer, displayAsTree) {
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
      size: 10,
      color: 'grey'
    }
  }

  var network = new vis.Network(targetContainer, data, options);
};
