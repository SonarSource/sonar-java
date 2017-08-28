jest
  .dontMock('jquery')
  .dontMock('vis');

const $ = require('jquery');
const vis = require('vis');
const viewer = require('../js/viewer.js');

global.$ = global.jQuery = $;

describe('viewer', function() {

  describe('table', function() {
    it('should generate a table without style if not provided', function() {
      const input = 'value';
      const output = viewer.table(input);
      expect(output).toBe('<table>value</table>');
    });

    it('should generate a table with a given style if not provided', function() {
      const input = 'value';
      const style = 'myStyle';
      const output = viewer.table(input, style);
      expect(output).toBe('<table class="myStyle">value</table>');
    });
  });

  describe('tableLine', function() {
    it('should generate a row with two cell when the value is provided', function() {
      const label = 'foo';
      const value = 'bar';
      const output = viewer.tableLine(label, value);
      expect(output).toBe('<tr><td>foo</td><td>bar</td></tr>');
    });

    it('should return an empty string when the value is not provided', function() {
      const label = 'foo';
      const value = null;
      const output = viewer.tableLine(label, value);
      expect(output).toBe('');
    });
  });

  describe('updateDetails', function() {
    it('should unescape labels', function() {
      const node = { id: 0, label: '&quot;ise&quest;&quot;'};
      const input = new vis.DataSet([node]);

      viewer.updateDetails(input);

      expect(input.get(0)).toEqual({id: 0, label: '"ise?"' });
    });

    it('should do nothing if there is no details nor label provided', function() {
      const node = {id: 0, otherdata: { stuff: 42}};
      const input = new vis.DataSet([node]);

      viewer.updateDetails(input);

      // no change
      expect(input.get(0)).toEqual({id: 0, otherdata: { stuff: 42}});
    });

    it('should convert details from escaped JSON format to real JSON object', function() {
      const node = {id: 0, details: '{ ?stuff?: ?42?, ?otherStuff?: { ?label? : ?foo? } }'};
      const input = new vis.DataSet([node]);

      viewer.updateDetails(input);

      // details is now a json object
      expect(input.get(0)).toEqual({ id: 0, details: {stuff: '42', otherStuff: { label: 'foo'}}});
    });
  });

  describe('getLearnedAssociations', function() {
    it('should generate an empty table if there is no learned associations', function() {
      const input = [];
      const output = viewer.getLearnedAssociations(input);
      expect(output).toBe('<h3>Learned associations</h3><table></table>');
    });

    it('should generate table with all learned associations  in correct order', function() {
      const input = [
        { sv : 'SV_1', symbol: 'a' },
        { symbol: 'b', sv : 'SV_2' }
      ];
      const output = viewer.getLearnedAssociations(input);
      expect(output).toBe('<h3>Learned associations</h3><table><tr><td>a</td><td>SV_1</td></tr><tr><td>b</td><td>SV_2</td></tr></table>');
    });
  });

  describe('getLearnedConstraints', function() {
    it('should generate an empty table if there is no learned constraints', function() {
      const input = [];
      const output = viewer.getLearnedConstraints(input);
      expect(output).toBe('<h3>Learned constraints</h3><table></table>');
    });

    it('should generate table with all learned constraints in correct order', function() {
      const input = [
        { sv : 'SV_1', constraint: 'NULL' },
        { constraint: 'NOT_NULL', sv : 'SV_2' }
      ];

      const output = viewer.getLearnedConstraints(input);
      expect(output).toBe('<h3>Learned constraints</h3><table><tr><td>SV_1</td><td>NULL</td></tr><tr><td>SV_2</td><td>NOT_NULL</td></tr></table>');
    });
  });

  describe('getMethodYield', function() {
    it('should return a table row describing the yield (happy yield returning param)', function() {
      const yieldId = 42;
      const input = {
          params: [['NOT_NULL', 'TRUE'], ['no constraint']],
          result: ['NULL'],
          resultIndex: 1
      };
      const output = viewer.getMethodYield(input, yieldId);
      expect(output).toBe('<tr>'
          + '<td>#42</td>'
          + '<td><table class=\"innerTable\">'
            + '<tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr>'
            + '<tr><td>arg1</td><td>no constraint</td></tr>'
          + '</table></td>'
          + '<td>NULL (arg1)</td>'
          + '</tr>');
    });

    it('should return a table row describing the yield (happy yield not returning)', function() {
      const yieldId = 42;
      const input = {
          params: [['NOT_NULL', 'TRUE'], ['no constraint']],
          result: ['no constraint'],
          resultIndex: -1
      };
      const output = viewer.getMethodYield(input, yieldId);
      expect(output).toBe('<tr>'
          + '<td>#42</td>'
          + '<td><table class=\"innerTable\">'
            + '<tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr>'
            + '<tr><td>arg1</td><td>no constraint</td></tr>'
          + '</table></td>'
          + '<td>no constraint</td>'
          + '</tr>');
    });

    it('should return a table row describing the yield (exceptional yield)', function() {
      const yieldId = 42;
      const input = {
          params: [['NOT_NULL', 'TRUE'], ['no constraint']],
          exception: ['org.foo.MyException']
      };
      const output = viewer.getMethodYield(input, yieldId);
      expect(output).toBe('<tr>'
          + '<td>#42</td>'
          + '<td><table class=\"innerTable\">'
            + '<tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr>'
            + '<tr><td>arg1</td><td>no constraint</td></tr>'
          + '</table></td>'
          + '<td><code>org.foo.MyException</code></td>'
          + '</tr>');
    });
  });

  describe('getEdgeDetails', function() {
    it ('should return "No data" when nothing is provided', function() {
      const input = null;
      const output = viewer.getEdgeDetails(input);

      expect(output).toEqual(expect.stringContaining('No data'));
    });

    it ('should return "No data" when details does not contains what is expected', function() {
      const input = { otherStuff: 42 };
      const output = viewer.getEdgeDetails(input);

      expect(output).toEqual(expect.stringContaining('No data'));
    });

    it ('should generates table with learned constraints when provided', function() {
      const input = { learnedConstraints: [] };
      const output = viewer.getEdgeDetails(input);

      expect(output).toBe('<h3>Learned constraints</h3><table></table>');
    });

    it ('should generates table with learned associations when provided', function() {
      const input = { learnedAssociations: [] };
      const output = viewer.getEdgeDetails(input);

      expect(output).toBe('<h3>Learned associations</h3><table></table>');
    });

    it ('should generates table with yields when provided', function() {
      const input = { selectedMethodYields: [{
          params: [['NOT_NULL', 'TRUE'], ['no constraint']],
          exception: ['org.foo.MyException']
        }, {
          params: [['NOT_NULL', 'TRUE'], ['no constraint']],
          result: ['NULL'],
          resultIndex: -1
        }]
      };
      const output = viewer.getEdgeDetails(input);

      expect(output).toBe('<h3>Selected method yields</h3><table class=\"methodYield-table\"><tr><th></th><th>Parameters constraints</th><th>Result constraint / Thrown exception</th></tr><tr><td>#1</td><td><table class=\"innerTable\"><tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr><tr><td>arg1</td><td>no constraint</td></tr></table></td><td><code>org.foo.MyException</code></td></tr><tr><td>#2</td><td><table class=\"innerTable\"><tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr><tr><td>arg1</td><td>no constraint</td></tr></table></td><td>NULL</td></tr></table>');
    });

    it ('should add separators when 2 options are provided (associations + constraints)', function() {
      const input = {
        learnedAssociations: [],
        learnedConstraints: []
      };
      const output = viewer.getEdgeDetails(input);

      expect(output).toEqual(expect.stringMatching(/.*(Learned constraints).*(<hr>).*(Learned associations).*/));
    });

    it ('should add separators when 2 options are provided (associations + yields)', function() {
      const input = {
        selectedMethodYields: [],
        learnedAssociations: []
      };
      const output = viewer.getEdgeDetails(input);

      expect(output).toEqual(expect.stringMatching(/.*(Learned associations).*(<hr>).*(Selected method yields).*/));
    });

    it ('should add separators when 2 options are provided (constraints + yields)', function() {
      const input = {
        selectedMethodYields: [],
        learnedConstraints: []
      };
      const output = viewer.getEdgeDetails(input);

      expect(output).toEqual(expect.stringMatching(/.*(Learned constraints).*(<hr>).*(Selected method yields).*/));
    });
  });

  describe('getNodeDetails', function() {
    it('should return "no data" when no details are provided', function() {
      const input = null;
      const output = viewer.getNodeDetails(input);
      expect(output).toEqual(expect.stringContaining('No data'));
    });

    it('should contains method yields when provided', function() {
      const input = {
          methodName: 'foo',
          methodYields: [{
            params: [['NOT_NULL', 'TRUE'], ['no constraint']],
            exception: ['org.foo.MyException']
          }, {
            params: [['NOT_NULL', 'TRUE'], ['no constraint']],
            result: ['NULL'],
            resultIndex: -1
          }
      ]};
      const output = viewer.getNodeDetails(input);
      expect(output).toEqual(expect.stringContaining('<h3>Program State</h3>'
          + '<table class=\"programState-table\"></table>'
          + '<hr>'
          + '<h3>Method Yields: foo(...)</h3>'
          + '<table class=\"methodYield-table\">'
            + '<tr><th></th><th>Parameters constraints</th><th>Result constraint / Thrown exception</th></tr>'
            + '<tr><td>#1</td><td><table class=\"innerTable\"><tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr><tr><td>arg1</td><td>no constraint</td></tr></table></td><td><code>org.foo.MyException</code></td></tr>'
            + '<tr><td>#2</td><td><table class=\"innerTable\"><tr><td>arg0</td><td>NOT_NULL,TRUE</td></tr><tr><td>arg1</td><td>no constraint</td></tr></table></td><td>NULL</td></tr>'
          + '</table>'));
    });
  });

  describe('getProgramState', function() {
    it('should provide details for known values and constraints', function() {
      const input = {
        psValues: [
          {sv: 'SV_42', symbol: 'a' },
          {symbol: 'b', sv: 'SV_21' }
        ],
        psConstraints: [
          {sv: 'SV_42', constraints: ['NOT_NULL', 'OPEN']},
          {constraints: ['NOT_NULL', 'FALSE'], sv: 'SV_57'}
        ],
        psStack: []
      };
      const output = viewer.getNodeDetails(input);
      expect(output).toEqual(expect.stringContaining('<h3>Program State</h3>'
          + '<table class=\"programState-table\">'
          + '<tr><td>Values</td><td><table class=\"innerTable\"><tr><td>a</td><td>SV_42</td></tr><tr><td>b</td><td>SV_21</td></tr></table></td></tr>'
          + '<tr><td>Constraints</td><td><table class=\"innerTable\"><tr><td>SV_42</td><td>NOT_NULL,OPEN</td></tr><tr><td>SV_57</td><td>NOT_NULL,FALSE</td></tr></table></td></tr>'
          + '<tr><td>Stack</td><td><ul class=\"list-group\"><li class=\"list-group-item\"><em>empty</em></li></ul></td></tr>'
          + '</table>'));
    });

    it('should provide details for the stack differently', function() {
      const input = {
        psValues: [],
        psConstraints: [],
        psStack: [
          {sv: 'SV_42', symbol: 'a' },
          // symbol is not required
          {sv: 'SV_21' }
        ]
      };
      const output = viewer.getNodeDetails(input);
      expect(output).toEqual(expect.stringContaining('<h3>Program State</h3>'
          + '<table class=\"programState-table\">'
          + '<tr><td>Values</td><td><table class=\"innerTable\"></table></td></tr>'
          + '<tr><td>Constraints</td><td><table class=\"innerTable\"></table></td></tr>'
          + '<tr><td>Stack</td><td><ul class=\"list-group\">'
            + '<li class=\"list-group-item\">SV_42<sub>a</sub></li>'
            + '<li class=\"list-group-item\">SV_21<sub></sub></li>'
          + '</ul></td></tr>'
          + '</table>'));
    });
  });

  describe('setNodesColor', function() {

    const DEFAULT_COLOR = '#eee';

    it('should apply default color and font to all nodes without explicit highlighting', function() {
      const nodes = [{id: 0}];
      const input = new vis.DataSet(nodes);

      viewer.setNodesColor(input);

      const newNode = input.get(0);
      expect(newNode).toHaveProperty('color', expect.any(Object));
      expect(newNode).toHaveProperty('color.background', DEFAULT_COLOR);
      expect(newNode).toHaveProperty('font', expect.any(Object));
    });

    it('should apply other color than default as soon as there is a recognized highlighting', function() {
      const nodes = [
        {id: 0, highlighting: 'firstNode'},
        {id: 1, highlighting: 'exitNode'},
        {id: 2, highlighting: 'lostNode'},
        {id: 3, highlighting: 'tokenKind'},
        {id: 4, highlighting: 'classKind'},
        {id: 5, highlighting: 'methodKind'},
        {id: 6, highlighting: 'samePP'},
        {id: 7, highlighting: 'not_a_known_highlighting'}
      ];
      const input = new vis.DataSet(nodes);

      viewer.setNodesColor(input);

      input.forEach(function (node) {
        expect(node).toHaveProperty('color.background', expect.any(String));
        if (node.highlighting === 'not_a_known_highlighting') {
          expect(node.color.background).toEqual(DEFAULT_COLOR);
        } else {
          expect(node.color.background).not.toEqual(DEFAULT_COLOR);
        }
      });
    });

    it('should be able to force color even if highlighting is present', function() {
      const nodes = [
        {id: 0, highlighting: 'firstNode'},
        {id: 1},
        {id: 2}
      ];
      const input = new vis.DataSet(nodes);

      viewer.setNodesColor(input, [0, 1], 'samePP');

      input.forEach(function (node) {
        expect(node).toHaveProperty('color.background', expect.any(String));
        if (node.id === 0 || node.id === 1) {
          expect(node.color.background).not.toEqual(DEFAULT_COLOR);
        } else {
          expect(node.color.background).toEqual(DEFAULT_COLOR);
        }
      });
    });
  });

  describe('setEdgesColor', function() {

    const DEFAULT_COLOR = 'gray';

    it('should apply default color and font to all edge without explicit highlighting', function() {
      const edges = [{id: 0}];
      const input = new vis.DataSet(edges);

      viewer.setEdgesColor(input);

      const newEdge = input.get(0);
      expect(newEdge).toHaveProperty('color', expect.any(Object));
      expect(newEdge).toHaveProperty('color.color', DEFAULT_COLOR);
      expect(newEdge).toHaveProperty('font', expect.any(Object));
    });

    it('should apply other color than default as soon as there is a recognized highlighting', function() {
      const edges = [
        {id: 0, highlighting: 'exceptionEdge'},
        {id: 1, highlighting: 'yieldEdge'},
        {id: 7, highlighting: 'not_a_known_highlighting'}
      ];
      const input = new vis.DataSet(edges);

      viewer.setEdgesColor(input);

      input.forEach(function (edge) {
        expect(edge).toHaveProperty('color.color', expect.any(String));
        if (edge.highlighting === 'not_a_known_highlighting') {
          expect(edge.color.color).toEqual(DEFAULT_COLOR);
        } else {
          expect(edge.color.color).not.toEqual(DEFAULT_COLOR);
        }
      });
    });
  });

  describe('changeLayout', function() {
    it('should not set hierarchical layout by default', function() {
      let result;
      const mockNetwork = {
        setOptions(o) {
          result = o;
        }
      };

      viewer.changeLayout(mockNetwork);
      expect(result).toHaveProperty('layout.hierarchical', false);
    });

    it('should set hierarchical layout when asked', function() {
      let result;
      const mockNetwork = {
        setOptions(o) {
          result = o;
        }
      };

      viewer.changeLayout(mockNetwork, true);
      expect(result).toHaveProperty('layout.hierarchical', expect.any(Object));
      expect(result).toHaveProperty('layout.hierarchical.enabled', true);
    });
  });

  describe('mapPPByLine', function() {
    it('should return an empty map in case of empty string', function() {
      const input = '';
      const output = viewer.mapPPByLine(input);
      expect(output).toEqual({});
    });

    it('should return an empty map in case of non-cfg text', function() {
      const input = 'foo\nbar\qix';
      const output = viewer.mapPPByLine(input);
      expect(output).toEqual({});
    });

    it('should build a map of program point by line', function() {
      const input = 'Starts at B2\n'
        + '\n'
        + 'B2\n'
        + '0:  IDENTIFIER                            bar\n'
        + '1:  IDENTIFIER                            a\n'
        + '2:  IDENTIFIER                            b\n'
        + '3:  METHOD_INVOCATION                     bar ( a , b )\n'
        + '4:  VARIABLE                              Object o\n'
        + '5:  IDENTIFIER                            a\n'
        + 'T:  IF_STATEMENT                          if ( a )\n'
        + '  jumps to: B1(true) B0(false)\n'
        + '\n'
        + 'B1\n'
        + '0:  IDENTIFIER                            o\n'
        + '1:  METHOD_INVOCATION                     o . toString ( )\n'
        + '  jumps to: B0\n'
        + '\n'
        + 'B0 (Exit):\n\n';
      const output = viewer.mapPPByLine(input);
      expect(output).toEqual({
        'B2.0': 3,
        'B2.1': 4,
        'B2.2': 5,
        'B2.3': 6,
        'B2.4': 7,
        'B2.5': 8,
        'B1.0': 13,
        'B1.1': 14,
        'B0.0': 17
      });
    });
  });

  describe('getPPMap', function() {
    it('should return an empty map if there is no nodes', function() {
      const nodes = [];
      const input = new vis.DataSet(nodes);
      const output = viewer.getPPMap(input);
      expect(output).toEqual({});
    });

    it('should return an empty map if there is no ppKey provided', function() {
      const nodes = [
        {id: 0},
        {id: 1, details: {}}
      ];
      const input = new vis.DataSet(nodes);
      const output = viewer.getPPMap(input);
      expect(output).toEqual({});
    });

    it('should return a map of node ids by ppKey', function() {
      const nodes = [
        {id: 0, details: {ppKey: 'B2.0'}},
        {id: 1, details: {ppKey: 'B1.0'}},
        {id: 2, details: {ppKey: 'B1.0'}}
      ];
      const input = new vis.DataSet(nodes);
      const output = viewer.getPPMap(input);
      expect(output).toEqual({
        'B2.0' : [0],
        'B1.0' : [1, 2]
      });
    });
  });

  describe('highlightAllNodesAtSamePP', function() {
    it('should highlight nodes having same PP, update cfg editor and redraw graph', function() {
      const ppKey = 'B1.0';
      const nodes = new vis.DataSet([
        {id: 0, details: {ppKey: 'B2.0'}},
        {id: 1, details: {ppKey: 'B1.0'}},
        {id: 2, details: {ppKey: 'B1.0'}},
        {id: 3, details: {ppKey: 'B0.0'}}
      ]);
      const nodeIdsWithSamePP = [1, 2];

      let editorCursorLine = -1;
      let redrawn = false;
      const mockNetwork = {
        eg : {
          ppMapCFG: {
            'B2.0' : -1,
            'B1.0' : 42,
            'B0.0' : -1
          },
          cfgEditor: {
            setCursor(line) {
              editorCursorLine = line;
            }
          }
        },
        redraw() {
          redrawn = true;
        }
      };

      viewer.highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, nodes, mockNetwork);

      // nodes should have been highlighted
      expect(nodes.get(1)['color']['background']).toEqual('pink');

      // network should have been redrawn
      expect(redrawn).toEqual(true);

      // knowledge of selected line in network should be updated
      expect(mockNetwork['eg']['cfgEditorSelectedLine']).toEqual(42);

      // should have called editor to select line
      expect(editorCursorLine).toEqual(42);
    });

    it('should highlight nodes having same PP and redraw graph but not update editor if corresponding line is unknown in editor', function() {
      const ppKey = 'B1.0';
      const nodes = new vis.DataSet([
        {id: 'a', details: {ppKey: 'B2.0'}},
        {id: 'b', details: {ppKey: 'B1.0'}},
        {id: 'c', details: {ppKey: 'B1.0'}},
        {id: 'd', details: {ppKey: 'B0.0'}}
      ]);
      const nodeIdsWithSamePP = ['b', 'c'];

      let editorCursorLine = -1;
      let redrawn = false;
      const mockNetwork = {
        eg : {
          ppMapCFG: {
            'B2.0' : 42,
            'B0.0' : 42
          },
          cfgEditor: {
            setCursor(line) {
              editorCursorLine = line;
            }
          }
        },
        redraw() {
          redrawn = true;
        }
      };

      viewer.highlightAllNodesAtSamePP(ppKey, nodeIdsWithSamePP, nodes, mockNetwork);

      // nodes should have been highlighted
      expect(nodes.get('b')['color']['background']).toEqual('pink');
      expect(nodes.get('c')['color']['background']).toEqual('pink');

      // network should have been redrawn
      expect(redrawn).toEqual(true);

      // knowledge of selected line in network should not be updated
      expect(mockNetwork['eg']['cfgEditorSelectedLine']).toBeUndefined();

      // should not have called editor to select line
      expect(editorCursorLine).toEqual(-1);
    });
  });

  describe('handleNewPP', function() {
    it('should highlight nothing in CFG editor if already on the same line', function() {
      const mockEditor = { getCursor() { return {line: 42}; }};
      const mockNetwork = { eg: { cfgEditorSelectedLine: 42 }};

      viewer.handleNewPP(mockEditor, mockNetwork);

      expect(mockNetwork.eg.cfgEditorSelectedLine).toEqual(42);
    });

    it('should highlight nodes when selecting a line in editor corresponding to a Program Point', function() {
      let editorNewLine = -1;
      const mockEditor = {
        getCursor() { return {line: 42}; },
        setCursor(line) {
          editorNewLine = line;
        }
      };
      const mockNetwork = {
        eg: {
          cfgEditorSelectedLine: -1,
          cfgEditor: mockEditor,
          ppMap: {
            'B2.0' : ['a'],
            'B1.0' : ['b','c'],
            'B0.0' : ['d']
          },
          nodes: new vis.DataSet([
            {id: 'a', details: {ppKey: 'B2.0'}},
            {id: 'b', details: {ppKey: 'B1.0'}},
            {id: 'c', details: {ppKey: 'B1.0'}},
            {id: 'd', details: {ppKey: 'B0.0'}}
          ]),
          ppMapCFG: {
            'B2.0' : -1,
            'B1.0' : 42,
            'B0.0' : -1
          }
        },
        redraw() { /* NOP */ }
      };

      viewer.handleNewPP(mockEditor, mockNetwork);

      // various line markers updated
      expect(mockNetwork.eg.cfgEditorSelectedLine).toEqual(42);
      expect(editorNewLine).toEqual(42);

      // nodes should have been highlighted
      expect(mockNetwork.eg.nodes.get('b')['color']['background']).toEqual('pink');
      expect(mockNetwork.eg.nodes.get('c')['color']['background']).toEqual('pink');
    });

    it('should highlight nothing if line from CFG editor does not match any PP', function() {
      const NODE_DEFAULT_COLOR = '#eee';
      let editorNewLine = -1;
      const mockEditor = {
        getCursor() { return {line: 42}; },
        setCursor(line) {
          editorNewLine = line;
        }
      };
      const mockNetwork = {
        eg: {
          cfgEditorSelectedLine: -1,
          cfgEditor: mockEditor,
          ppMap: {
            'B2.0' : ['a'],
            'B1.0' : ['b','c'],
            'B0.0' : ['d']
          },
          nodes: new vis.DataSet([
            {id: 'a', details: {ppKey: 'B2.0'}},
            {id: 'b', details: {ppKey: 'B1.0'}},
            {id: 'c', details: {ppKey: 'B1.0'}},
            {id: 'd', details: {ppKey: 'B0.0'}}
          ]),
          ppMapCFG: {
            'B2.0' : -1,
            'B1.0' : -1,
            'B0.0' : -1
          }
        },
        redraw() { /* NOP */ }
      };

      viewer.handleNewPP(mockEditor, mockNetwork);

      // various line markers updated
      expect(mockNetwork.eg.cfgEditorSelectedLine).toEqual(42);

      // should not be called
      expect(editorNewLine).toEqual(-1);

      // nodes should have been highlighted
      expect(mockNetwork.eg.nodes.get('a')['color']['background']).toEqual(NODE_DEFAULT_COLOR);
      expect(mockNetwork.eg.nodes.get('b')['color']['background']).toEqual(NODE_DEFAULT_COLOR);
      expect(mockNetwork.eg.nodes.get('c')['color']['background']).toEqual(NODE_DEFAULT_COLOR);
      expect(mockNetwork.eg.nodes.get('d')['color']['background']).toEqual(NODE_DEFAULT_COLOR);
    });
  });

  describe('clickAction', function() {
    it('should hide everything when nothing is selected', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([]),
        edges: new vis.DataSet([])
      };
      const params = {
        nodes: [],
        edges: []
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['node'].hidden).toBe(true);
      expect(detailsPanels['edge'].hidden).toBe(true);
    });

    it('should hide everything when more than one edge or more than one node is selected', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([]),
        edges: new vis.DataSet([])
      };
      const params = {
        nodes: [1, 2, 3],
        edges: [4, 5, 6]
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['node'].hidden).toBe(true);
      expect(detailsPanels['edge'].hidden).toBe(true);
    });

    it('should hide everything when the node does not exist', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([]),
        edges: new vis.DataSet([])
      };
      const params = {
        nodes: [42],
        edges: []
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['node'].hidden).toBe(true);
      expect(detailsPanels['edge'].hidden).toBe(true);
    });

    it('should hide everything when the edge does not exist', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([]),
        edges: new vis.DataSet([])
      };
      const params = {
        nodes: [],
        edges: [42]
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['node'].hidden).toBe(true);
      expect(detailsPanels['edge'].hidden).toBe(true);
    });

    it('should select one edge when having only one edge id', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([]),
        edges: new vis.DataSet([{id: 0}])
      };
      const params = {
        nodes: [],
        edges: [0]
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['node'].hidden).toBe(true);

      expect(detailsPanels['edge'].hidden).toBe(false);
      expect(detailsPanels['edge'].panel.htmlValue).toEqual(expect.stringContaining('No data'));
      expect(detailsPanels['edge'].panel.collapsed).toBe('show');
    });

    it('should select one node when having only one node id', function() {
      const detailsPanels = {
        'info' : mockDetailsPanel(),
        'node' : mockDetailsPanel(),
        'edge' : mockDetailsPanel()
      };
      const data = {
        nodes: new vis.DataSet([{id: 0}]),
        edges: new vis.DataSet([])
      };
      const params = {
        nodes: [0],
        edges: []
      };

      viewer.clickAction(params, data, undefined, detailsPanels);

      expect(detailsPanels['info'].hidden).toBe(true);
      expect(detailsPanels['edge'].hidden).toBe(true);

      expect(detailsPanels['node'].hidden).toBe(false);
      expect(detailsPanels['node'].panel.htmlValue).toEqual(expect.stringContaining('No data'));
      expect(detailsPanels['node'].panel.collapsed).toBe('show');
    });

    function mockDetailsPanel() {
      return {
        panel: {
          htmlValue: null,
          collapsed: null,
          html(value) {
            this.htmlValue = value;
          },
          collapse(value) {
            this.collapsed = value;
          }
        },
        hidden: undefined,
        hide() {
          this.hidden = true;
        },
        show() {
          this.hidden = false;
        },
        find(value) {
          return this.panel;
        }
      };
    }
  });
});
