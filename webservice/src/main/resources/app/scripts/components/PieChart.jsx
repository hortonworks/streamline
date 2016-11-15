import React, {Component} from 'react';
import d3 from 'd3';

export default class PieChart extends Component {
    constructor(props){
        super(props)
    }
    initGraph = () => {
        this.width = this.refs.svg.parentElement.clientWidth;
        this.height = this.refs.svg.parentElement.clientHeight;

        this.svg = d3.select(this.refs.svg);

        this.container = this.svg.append('g');
    }
    setRadius () {
        this.radius = Math.min(this.width, this.height) / 2;
    }
    setArc(){
        this.arc = d3.svg.arc()
            .outerRadius(this.radius - this.props.outerRadius)
            .innerRadius(this.radius - this.props.innerRadius)
            .startAngle( function ( d ) { return isNaN( d.startAngle ) ? 0 : d.startAngle; })
            .endAngle( function ( d ) { return isNaN( d.endAngle ) ? 0 : d.endAngle; });
    }
    setPie(){
        this.pie = d3.layout.pie()
            .sort(null)
            .value(function(d) {
                return 0;
            });
    }
    drawChart(){
        this.setRadius();
        this.setArc();
        this.setPie();
        this.drawPie();
    }
    drawPie(){
        var self = this;
        this.container.attr("transform", "translate(" + this.width / 2 + "," + this.height / 2 + ")");

        this.container.datum(this.props.data)
        this.arc_g = this.container.selectAll(".arc")
            .data(this.pie)

            // .data(this.pie(this.data))
        this.arc_g
            .exit()
            .remove()

        this.arc_g
            .enter().append("g")
            .style("fill", function(d) {
                return self.props.color(d.data.name);
            })
            .attr("class", "arc");

        this.paths = this.arc_g.selectAll('path').data(function(d){
            return [d];
        })

        this.paths
            .exit()
            .remove()

        this.paths.enter()
        .append("path")
            .each(function(d) {
                this._current = d; })
            .attr("d", this.arc)


        if(this.props.showLabels){
            this.texts = this.arc_g.selectAll('text').data(function(d){
                return [d];
            })

            this.texts
                .exit()
                .remove()

            this.texts
                .enter()
                .append("text")
                .attr("transform", function(d) {
                    return "translate(" + self.arc.centroid(d) + ")";
                })
                .attr("dy", ".35em")
                .style('fill','black')
                .style('text-anchor', 'middle')
                .text(function(d) {
                    return d.data.name;
                });
        }

        this.titles = this.arc_g.selectAll('title').data(function(d){
            return [d]
        })

        this.titles
            .exit()
            .remove()

        this.titles
            .enter()
            .append("title")

        this.transition();
    }
    transition(){
        var self = this;
        function arcTween(a){
            var i = d3.interpolate(this._current, a);
            this._current = i(0);
            return function(t) {
                return self.arc(i(t));
            }
        }
        this.pie.value(function(d){
            return d.value;
        })
        this.paths.data(function(d, i){
            return [self.pie(self.props.data)[i]];
        });
        this.paths.transition().duration(500)
            .attrTween("d", arcTween)

        if(this.props.showLabels){
            this.texts.data(function(d, i){
                return [self.pie(self.props.data)[i]];
            });
            this.texts.transition().duration(200).delay(500)
                .attr("transform", function(d) {
                    return "translate(" + self.arc.centroid(d) + ")";
                })
        }

        this.titles
            .text(function(d){
                return d.data.name + ' : ' + d.data.value.toLocaleString();
            })
    }
    componentDidUpdate(){
        this.drawChart()
    }
    componentDidMount(){
        this.initGraph()
        this.drawChart()
    }
    render(){
        return <svg ref="svg" width="100%" height="100%"></svg>
    }
}

PieChart.defaultProps = {
    outerRadius: 0,
    innerRadius: 30,
    showLabels: false,
    data: [],
    color: d3.scale.category20c()
}
