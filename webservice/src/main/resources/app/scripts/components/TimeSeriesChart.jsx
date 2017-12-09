/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component} from 'react';
import d3 from 'd3';
import _ from 'lodash';

export default class TimeSeriesChart extends Component {
  constructor(props) {
    super(props);
  }
  componentDidUpdate() {
    this.drawChart();

    this.props.drawBrush.call(this);
  }
  componentDidMount() {
    this.initGraph();
    this.drawChart();
    // this.props.drawXAxis.call(this);
    // this.props.drawYAxis.call(this);
    this.props.drawBrush.call(this);

    this.props.initTooltip.call(this);

    this.container.selectAll('.axis path.domain').classed('hidden', true);
  }
  render() {
    return <svg ref="svg" width="100%" height="100%"></svg>;
  }
  initGraph() {
    this.svg = d3.select(this.refs.svg);

    this.container = this.svg.append('g');

    this.pathGrp = this.container.append('svg');

    this.brush_g = this.container.append("g").attr("class", "x brush");
  }
  drawChart() {
    this.width = this.props.width || this.refs.svg.parentElement.clientWidth || this.width || 0;
    this.height = this.props.height || this.refs.svg.parentElement.clientHeight || this.width || 0;

    //this.svg.attr("viewBox", "-46 -6 " + (this.width + 82) + " " + (this.height + 27)).attr("preserveAspectRatio", "xMidYMid");

    this.pathGrp.attr('width', this.width).attr('height', this.height);

    this.x = this.props.getXScale.call(this);
    this.y = this.props.getYScale.call(this);
    this.xAxis = this.props.getXAxis.call(this);
    this.yAxis = this.props.getYAxis.call(this);
    this.props.setColorDomain.call(this);

    this.mapedData = this.props.mapData.call(this);

    this.props.setXDomain.call(this);
    this.props.setYDomain.call(this);

    this.area = this.props.getSvgArea.call(this);
    this.lines = this.props.getSvgLines.call(this);
    this.props.drawLines.call(this);
    this.props.updateXAxis.call(this);
    this.props.updateYAxis.call(this);
  }

}

TimeSeriesChart.defaultProps = {
  bars: [],
  xAttr: 'date',
  data: [
    {
      date: new Date('11/02/2016'),
      value: 10,
      value2: 15
    }, {
      date: new Date('11/03/2016'),
      value: 16,
      value2: 14
    }, {
      date: new Date('11/04/2016'),
      value: 8,
      value2: 7
    }
  ],
  color: d3.scale.category20c(),
  getXScale() {
    return d3.time.scale().range([0, this.width]);
  },
  getYScale() {
    return d3.scale.linear().range([
      this.height - 1,
      0
    ]);
  },
  getXAxis() {
    return d3.svg.axis().scale(this.x).orient("bottom").tickSize(-this.height, 0, 0);
  },
  getYAxis() {
    return d3.svg.axis().scale(this.y).orient("left").tickSize(-this.width, 0, 0);
  },
  setColorDomain() {
    this.props.color.domain(d3.keys(this.props.data[0]).filter((key) => {
      return key !== this.props.xAttr;
    }));
  },
  getSvgArea() {
    const self = this;
    return d3.svg.area().interpolate(self.props.interpolation).x(function(d) {
      return self.x(d[self.props.xAttr]);
    }).y0(this.height).y1(function(d) {
      return self.y(d.value);
    });
  },
  getSvgLines() {
    const self = this;
    return d3.svg.line().interpolate(self.props.interpolation).x(function(d) {
      return self.x(d[self.props.xAttr]);
    }).y(function(d) {
      return self.y(d.value);
    });
  },
  mapData() {
    return this.props.color.domain().map((name) => {
      return {
        name: name,
        values: this.props.data.map((d, i, arr) => {
          const mainArguments = [d, i, arr, name];
          // Array.prototype.slice.call(arguments);
          // mainArguments.push(name);
          return this.props.valuesFormat.apply(this, mainArguments);
        })
      };
    });
  },
  valuesFormat(d, a, b, name) {
    var tempObj = {};
    tempObj[this.props.xAttr] = d[this.props.xAttr];
    tempObj.name = name;
    tempObj.value = d[name];
    return tempObj;
  },
  setXDomain() {
    var dateRange = this.boundaryDate = d3.extent(this.props.data, (d) => {
      return d[this.props.xAttr];
    });
    var diff = (dateRange[1] - dateRange[0]) / 60000 / this.props.data.length;
    var dateRangeAfterDiff = [
      d3.time.minute.offset(dateRange[0], -(diff / 2)),
      d3.time.minute.offset(dateRange[1], + (diff / 2))
    ];
    this.x.domain(dateRange);
  },
  setYDomain() {
    this.y.domain([
      d3.min(this.mapedData, (c) => {
        return d3.min(c.values, (v) => {
          return v.value;
        });
      }),
      d3.max(this.mapedData, (c) => {
        return d3.max(c.values, (v) => {
          return v.value;
        });
      })
    ]);
  },
  drawLines() {
    const self = this;

    const pathGrp = this.pathGrp.selectAll(".pathGroup").data(this.mapedData/*() => {
                return _.filter(this.mapedData, (d) => {
                    return true;
                    // return !_.contains(this.props.bars, d.name);
                })
            }*/);

    pathGrp.exit().remove();

    pathGrp.enter().append("g").attr("class", "pathGroup line").classed("visible", true).attr('transform', 'translate(0,1)').attr('data-name', (d) => {
      return d.name;
    }).each(function() {
      d3.select(this).append("path").classed("line", true);

      d3.select(this).append("path").classed("area", true);
    });

    pathGrp.select('path.line').attr("d", function(d) {
      return self.lines(d.values);
    }).style({
      stroke: (d) => {
        return this.props.color(d.name);
      },
      fill: 'none'
    });

    pathGrp.select('path.area').attr("d", function(d) {
      return self.area(d.values);
    }).style({
      fill: (d) => {
        return this.props.color(d.name);
      },
      'fill-opacity': 0.1
    });

  },
  drawXAxis() {
    let xAxis = this.xAxis,
      container = this.container,
      height = this.height;

    container['xAxisEl'] = container.insert("g", ":first-child").attr("class", "x axis").attr("transform", "translate(0," + this.height + ")").call(xAxis);
  },
  updateXAxis() {
    this.container.select(".x.axis").attr("transform", "translate(0," + this.height + ")");
    this.container.select(".x.axis").transition().duration(0).call(this.xAxis);
  },
  drawYAxis(x) {
    var yAxis = this.yAxis;
    this.yAxisGrp = this.container.insert("g", ":first-child").attr("class", "y axis");
    this.yAxisGrp.ticks = this.yAxisGrp.call(yAxis);
    this.yAxisGrp.append('text').text(this.yAttr).attr("text-anchor", "end").attr("y", 6).attr("dy", ".75em").attr("transform", "rotate(-90)").style("letter-spacing", "0.2").style("font-size", "10px");
  },
  updateYAxis() {
    this.container.select(".y.axis").transition().duration(0).call(this.yAxis);
  },
  drawBrush() {
    this.brush = d3.svg.brush().x(this.x).on("brush", () => {
      this.props.onBrush.call(this);
    }).on('brushstart', () => {
      this.props.onBrushStart.call(this);
    }).on('brushend', () => {
      this.props.onBrushEnd.call(this);
    });

    this.brush_g.call(this.brush).selectAll("rect").attr("y", 0).attr("height", this.height);
  },
  onBrush() {},
  onBrushStart() {},
  onBrushEnd() {},
  initTooltip() {
    const self = this;
    this.tooltip = d3.select('body'). //this.svg.append('foreignobject')
    append('div').style("position", "absolute").classed('graph-tip', true).style({"z-index": "10", visibility: 'hidden', "pointer-events": 'none'});

    this.tipcontainer = this.svg.append('g').classed('tip-g', true).attr("transform", "translate(" + 0 + "," + 0 + ")");

    this.tipcontainer.append('g').classed('tipLine-g', true).append('line').classed('tipline', true).style('stroke', '#aaa').style('visibility', 'hidden').style('pointer-events', 'none').attr('x1', 0).attr('x2', 0).attr('y1', 0).attr('y2', this.height);

    if (this.brush) {
      this.toolTipRect = this.container.select('.brush rect.background');
    } else {
      this.toolTipRect = this.container.append('rect')
        .classed('tooltip-container', true)
        .attr({width: this.width, height: this.height})
        .style({fill: 'transparent'});
    }
    this.toolTipRect.on('mousemove', function(und, i, j) {
      const data = self.props.getTipData.call(self, und, i, j, this);
      self.props.showTooltip.call(self, data);
    }).on('mouseout', () => {
      this.props.hideTooltip.call(this);
    });
  },
  showTooltip(data) {
    const self = this;

    var tipline = this.tipcontainer.select('.tipline');

    const clientLeft = this.x(data[this.props.xAttr]);

    if (clientLeft < 0 || clientLeft > this.width) {
      return;
    }
    tipline.attr('x1', clientLeft).attr('x2', clientLeft).attr('y1', 0).attr('y2', this.height);
    tipline.style('visibility', 'visible');

    const lineRect = tipline.node().getBoundingClientRect();
    const targetTopDiff = event.pageY - event.currentTarget.getBoundingClientRect().top;

    this.tooltip.html('');
    this.props.addTooltipContent.call(this, data);

    this.tooltip.style({
      top: (lineRect.top + targetTopDiff) + "px",
      left: (lineRect.left + 10) + "px",
      visibility: 'visible'
    });
  },
  hideTooltip() {
    this.tooltip.style({visibility: 'hidden'});
    this.tipcontainer.select('.tipline').style('visibility', 'hidden');
  },
  getTipData(und, i, j, rect) {
    var self = this;
    var bisectDate = d3.bisector(function(d) {
      return d[self.props.xAttr];
    }).left;
    var x0 = this.x.invert(d3.mouse(rect)[0]);
    var i = bisectDate(this.props.data, x0, 1);
    var d0 = this.props.data[i - 1];
    var d1 = this.props.data[i] || this.props.data[i - 1];
    if (d0 == undefined || d1 == undefined){
      return undefined;
    }
    var d = x0 - d0[self.props.xAttr] > d1[self.props.xAttr] - x0
      ? d1
      : d0;

    return d;
  },
  addTooltipContent(d) {
    var html = '<span><i class="fa fa-calendar" aria-hidden="true"></i> ' + d[this.props.xAttr].toLocaleDateString() + '</span><br><span><i class="fa fa-clock-o" aria-hidden="true"></i> ' + d[this.props.xAttr].toLocaleTimeString() + '</span><table class="tooltipTable" width="100%"><tbody>';
    _.each(d, (val, key) => {
      if (key != this.props.xAttr) {
        html += '<tr><td><i class="fa fa-minus" style="color:' + this.props.color(key) + '"></i>' + key + ' </td><td> ' + parseFloat(val.toFixed(2)) + '</td></tr>';
      }
    });
    html += '</tbody></table>';
    this.tooltip.html(html);
  }
};
